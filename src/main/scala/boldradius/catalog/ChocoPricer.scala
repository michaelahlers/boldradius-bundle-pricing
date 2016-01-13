package boldradius.catalog

import boldradius.catalog.Pricer.UnmatchedItemsException
import boldradius.scala.collection.{MaskedAll, MaskedSome}
import boldradius.syntax.scala.collection._
import com.typesafe.scalalogging.LazyLogging
import org.chocosolver.solver.Solver
import org.chocosolver.solver.constraints.IntConstraintFactory
import org.chocosolver.solver.trace.Chatterbox
import org.chocosolver.solver.variables.{IntVar, VariableFactory}
import squants.market.{Money, MoneyContext, USD}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.Future
import scalaz.Scalaz._

/**
 * @see [[http://choco-solver.org/user_guide/5_elements.html __Elements of Choco: Constraints over integer variables__]]
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

      if (remainders.nonEmpty) throw UnmatchedItemsException(remainders)

      def variableName(rule: Rule) = rule.id.value.toString

      val Occurrences: mutable.Buffer[IntVar] = mutable.Buffer.empty
      val weights: mutable.Buffer[Int] = mutable.Buffer.empty
      val energies: mutable.Buffer[Int] = mutable.Buffer.empty

      val solver = new Solver

      countByRule foreach { case (rule, bound) =>
        Occurrences += VariableFactory.bounded(variableName(rule), 0, bound, solver)

        weights += rule.SKUs.size

        val energy: BigDecimal = rule.cost to USD
        energies += (energy * 100).toIntExact
      }

      val Weight = VariableFactory.fixed("Weight", items.size, solver)
      val Cost = VariableFactory.bounded("Cost", 0, 10000, solver)

      val constraint = IntConstraintFactory.knapsack(Occurrences.toArray, Weight, Cost, weights.toArray, energies.toArray)

      solver.post(constraint)

      var costs: Set[Money] = Set.empty

      /* Indexes to help interpret solutions. */
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

          if (countByItemSKU == countByRuleItemSKU) costs += USD(BigDecimal(Cost.getValue) / 100)

        } while (solver.nextSolution())
      }

      Chatterbox.printSolutions(solver)

      costs.min
    }

}

object ChocoPricer {

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
