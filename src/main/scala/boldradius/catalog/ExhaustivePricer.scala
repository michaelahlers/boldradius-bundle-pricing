package boldradius.catalog

import boldradius.catalog.Pricer.UnmatchedItemsException
import boldradius.scala.collection._
import boldradius.syntax.scala.collection._
import com.typesafe.scalalogging.LazyLogging
import squants.market.Money

import scala.annotation.tailrec
import scala.concurrent.Future

class ExhaustivePricer
  extends Pricer
          with LazyLogging {

  def rulesByTier(rules: List[Rule]): List[(Int, List[Rule])] =
    rules
      .groupBy(_.SKUs.size)
      .toList
      .sortBy({ case (tier, _) => tier })
      .reverse


  type Items = List[Item]

  type TieredRules = List[(Int, List[Rule])]

  type Path = List[Rule]

  type Paths = List[Path]

  /**
   * @param tasks Work to be consumed.
   * @param remainders Items which have not yet been evaluated in any case.
   * @param paths “Solutions,” effectively leaf nodes of search tree, that represent paths fully-constituting the original items.
   *
   * @return Discovered `paths` once all tasks are done.
   */
  @tailrec
  private def solver(tasks: List[(Path, TieredRules, Items)], remainders: Set[Item], paths: Paths): Paths =
    tasks match {

      /** No further tasks to complete; return whatever solutions were found. */
      case Nil =>

        /** If there were items not accounted for, fail and report them. */
        if (remainders.nonEmpty) throw UnmatchedItemsException(/*Nil,*/ remainders)
        paths

      /** Items remain, but no more rules to evaluate. A subset of these items may appear in `remainders` and cause an error (if they're not matched later). */
      case (path, Nil, items) :: tail =>

        solver(tail, remainders, paths)

      /** Completed all rules in this tier. Evaluate same path and items with rules in the next tier. */
      case (path, (_, Nil) :: tiersTail, items) :: tail =>

        solver((path, tiersTail, items) +: tail, remainders, paths)

      /** Evaluate one rule from the given tier against given items at position denoted by teh path. */
      case (path, ((tier, rule :: rules) :: tieredRules), items) :: tail =>

        val mask = rule.SKUs.counted

        /** Upcoming tasks, reduced remainders, and added solutions. */
        val (nextTasks, nextRemainders, nextSolutions) =
          items.masked(mask, _.SKU) match {

            /** Current rule consumed everything; terminates processing. Add this path (__i.e.__, leaf node) to the solutions. */
            case MaskedAll(out) =>
              val nextPath = path :+ rule
              (tail, remainders -- out, paths :+ nextPath)

            /** Partial mask. Append this rule to the path, and offer this same rule back to tasks as it could match again. */
            case MaskedSome(in, out) =>
              val nextPath = path :+ rule
              val next = (nextPath, (tier, rule +: rules) +: tieredRules, in)
              (tail :+ next, remainders -- out, paths)

            /** Rule matched nothing (or empty items). Don't continue searching down this path. */
            case _ =>
              (tail, remainders, paths)

          }

        /** Offer remaining rules at this tier back to the task queue (advancing through rules). */
        val next = (path, (tier, rules) +: tieredRules, items)

        solver(nextTasks :+ next, nextRemainders, nextSolutions)

    }

  private def solver(rules: List[Rule], items: List[Item]): Paths =
    solver(List((Nil, rulesByTier(rules), items)), items.toSet, Nil)

  /** @inheritdoc*/
  override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
    Future.successful {
      val paths = solver(rules, items)
      paths.map(_.map(_.cost).reduce(_ + _)).min
    }

}
