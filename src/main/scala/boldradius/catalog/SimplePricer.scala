package boldradius.catalog

import boldradius.catalog.bundling.Rule
import boldradius.scala.collection.{MaskedAll, MaskedEmpty, MaskedNone, MaskedSome, _}
import com.typesafe.scalalogging.LazyLogging
import squants.market.Money

import scala.annotation.tailrec
import scala.concurrent.Future
import scalaz.syntax.std.map._

class SimplePricer
  extends Pricer
          with LazyLogging {

  private case class Node(rule: Option[Rule], concerns: List[Item])

  private def materialize(rules: List[Rule], items: List[Item]) = {
    def pathFor(stack: List[(Node, List[Rule])]): List[Rule] =
      stack.collect({ case (Node(Some(r), _), _) => r })

    @tailrec
    def build(stack: List[(Node, List[Rule])], results: Set[Map[Rule, Int]]): Set[Map[Rule, Int]] =
      stack match {

        /** Empty stack indicates all combinations have been explored. */
        case Nil =>
          results

        /** No more rules left to evaluate at this position in the path. */
        case (root, Nil) :: tail if root.concerns.isEmpty =>
          val path: List[Rule] = pathFor(stack)
          val result: Map[Rule, Int] = path.foldLeft(Map.empty[Rule, Int]) { case (a, rule) =>
            a.alter(rule)(_.map(_ + 1).orElse(Some(1)))
          }

          build(tail, results + result)

        /* TODO: Error handling. */
        case (_, Nil) :: tail =>
          build(tail, results)

        /** Evaluate the next in line rule for this node's concerns. */
        case (root, rh :: rt) :: tail =>
          val mask = rh.SKUs.counted
          root.concerns.masked(mask, _.SKU) match {

            /** This node has no concerns, and is therefore a leaf. Don't descend further, and stop evaluating rules. */
            case MaskedEmpty =>
              build((root, Nil) :: tail, results)

            /** This rule was entirely inapplicable. Continue by offering remaining rules back to the queue. */
            case MaskedNone(_) =>
              build((root, rt) :: tail, results)

            /** Every concern was masked. Descend further, adding a node for this rule, include it, but don't offer to evaluate any further rules. */
            case MaskedAll(_) =>
              val child = Node(Some(rh), Nil)
              build((child, Nil) ::(root, rt) :: tail, results)

            /** A few concerns remain for this node. Descend further, adding a node, include it, and offer to evaluate all rules again for remaining concerns. */
            case MaskedSome(in, _) =>
              val child = Node(Some(rh), in)
              build((child, rules) ::(root, rt) :: tail, results)

          }

      }

    build(List(Node(None, items) -> rules), Set.empty)

  }

  override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
    Future.successful {
      val costs =
        materialize(rules, items) map { solution =>
          solution
            .map({ case (rule, count) => rule.cost * count })
            .reduce(_ + _)
        }

      costs.min
    }

}
