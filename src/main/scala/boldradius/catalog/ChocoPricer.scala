package boldradius.catalog

import boldradius.catalog.Pricer.UnmatchedItemsException
import boldradius.scala.collection.{MaskedAll, MaskedSome}
import boldradius.syntax.scala.collection._
import com.typesafe.scalalogging.LazyLogging
import org.chocosolver.solver.Solver
import org.chocosolver.solver.constraints.IntConstraintFactory
import org.chocosolver.solver.variables.{IntVar, VariableFactory}
import squants.Money
import squants.market.{MoneyContext, USD}

import scala.annotation.tailrec
import scala.concurrent.Future
import scalaz.Scalaz._

/**
 * Shamelessly borrows from [[http://choco-solver.org/user_guide/5_elements.html#knapsack Choco's knapsack example]] to find all ways applicable rules could be combined to match the items (shopping cart). The solver computes prices for each solution, but this implementation filters out combinations that ''don't'' match the cart. This is a huge flaw, but correctable (possibly in a future version) and would allow the solver to find [[http://choco-solver.org/user_guide/3_solving.html#finding-one-optimal-solution one optimal solution]]. It also attempts to mitigate the damage by resolving only those rules that match the cart, and establish an upper bound for each.
 *
 * Note that ([[org.chocosolver.solver.constraints.nary.PropKnapsack]]), Choco's knapsack propagator, deals only with integer values for energy. As a result, rule costs must be converted. (Internally, they're converted to [[squants.market.USD United States Dollars]] and from there into pennies. Once all solutions are found, resulting pennies are converted back into USD. Later on, this class should allow callers to specify what currency they want back. For now, clients may do the same using [[squants.market.Money#in(Currency)]].
 *
 * @see [[http://choco-solver.org/user_guide/5_elements.html __Elements of Choco: Constraints over integer variables__]]
 * @see [[http://wikipedia.org/wiki/Knapsack_problem Knapsack problem]] ([[http://wikipedia.org Wikipedia]])
 */
class ChocoPricer
  extends Pricer
          with LazyLogging {

  type ItemSKU = String

  implicit def moneyContext: MoneyContext = squants.market.defaultMoneyContext

  /**
   * @inheritdoc
   */
  override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
    Future.successful {

      val (remainders, countByRule) = ChocoPricer.matcher(rules, items)

      /** Fail fast if the matcher finds orphans. */
      if (remainders.nonEmpty) throw UnmatchedItemsException(remainders)

      /** A common definition for mapping rules to Choco variable names. */
      def variableName(rule: Rule) = rule.id.value.toString

      /** MUTABLE. This buffer and its contents may change during iteration over solutions. */
      var Occurrences: List[IntVar] = Nil

      /** MUTABLE. */
      var weights: List[Int] = Nil

      /** MUTABLE. */
      var costs: List[Int] = Nil

      val solver = new Solver

      countByRule foreach { case (rule, bound) =>
        Occurrences = Occurrences :+ VariableFactory.bounded(variableName(rule), 0, bound, solver)

        weights = weights :+ rule.SKUs.size

        /** Represent cost in pennies to serve the energy value for [[org.chocosolver.solver.constraints.nary.PropKnapsack]]. */
        val cost: BigDecimal = rule.cost to USD
        costs = costs :+ (cost * 100).toIntExact
      }

      /** Fixed weight; only get solutions with the same total item count as the cart. */
      val Weight = VariableFactory.fixed("Weight", items.size, solver)

      /* FIXME: Allow clients to specify the upper cost boundary. Currently, 1,000 USD. */
      val Cost = VariableFactory.bounded("Cost", 0, 1000 * 100, solver)

      val constraint = IntConstraintFactory.knapsack(Occurrences.toArray, Weight, Cost, weights.toArray, costs.toArray)
      solver.post(constraint)

      /** MUTABLE. Accumulator of solved costs. */
      var solutions: Set[Money] = Set.empty

      /** Indexes to help interpret solutions. */
      val countByItemSKU: Map[ItemSKU, Int] = items.countBy(_.SKU)
      val ruleByVarName: Map[String, Rule] = rules.groupBy(variableName).mapValues(_.head)

      if (solver.findSolution()) {

        do {

          val countByRuleItemSKU: Map[ItemSKU, Int] =
            Occurrences
              .filter(0 < _.getValue)
              .map({ v =>
                val rule: Rule = ruleByVarName(v.getName)
                rule.SKUs.counted.mapValues(_ * v.getValue)
              })
              .reduceOption(_ |+| _)
              .getOrElse(Map())

          /** Filter-out spurious solutions. */
          if (countByItemSKU == countByRuleItemSKU)
            solutions += USD(BigDecimal(Cost.getValue) / 100)

        } while (solver.nextSolution())
      }

      /* Uncomment to see how well (or poorly) we did. */
      // Chatterbox.printSolutions(solver)

      solutions.min
    }

}

object ChocoPricer {

  /**
   * Find any items that aren't matched by rules (needed for error-reporting), and the most times any given rule could appear in all items.
   *
   * @see UnmatchedItemsException
   */
  def matcher(rules: List[Rule], items: List[Item]): (Set[Item], Map[Rule, Int]) = {

    @tailrec
    def counter(queue: List[(Rule, List[Item])], remainders: Set[Item], occurrences: Map[Rule, Int]): (Set[Item], Map[Rule, Int]) =
      queue match {

        case Nil =>
          remainders -> occurrences

        case (r, is) :: tail =>
          is.masked(r.SKUs.counted, _.SKU) match {

            case MaskedAll(out) =>
              counter(tail, remainders -- out, occurrences |+| Map(r -> 1))

            case MaskedSome(in, out) =>
              counter((r -> in) +: tail, remainders -- out, occurrences |+| Map(r -> 1))

            case _ =>
              counter(tail, remainders, occurrences)

          }

      }

    counter(rules.map(_ -> items), items.toSet, Map.empty)

  }

}
