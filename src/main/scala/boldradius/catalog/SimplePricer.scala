package boldradius.catalog

import boldradius.catalog.Pricer.UnmatchedItemsException
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

  sealed trait Node {
    def concerns: List[Item]
  }

  case class Root(concerns: List[Item]) extends Node

  case class Internal(rule: Rule, concerns: List[Item]) extends Node

  case class Leaf(rule: Rule, concerns: List[Item] = Nil) extends Node

  def materialize(rules: List[Rule], cart: List[Item]) = {

    def pathFor(stack: List[(Node, List[Rule])]): List[Rule] =
      stack collect {
        case (Internal(r, _), _) => r
        case (Leaf(r, _), _) => r
      }

    @tailrec
    def build(stack: List[(Node, List[Rule])], matches: Set[Item], results: Set[Map[Rule, Int]]): Set[Map[Rule, Int]] =
      stack match {

        /** Arriving at the root indicates all matches have been explored. */
        case Nil | (Root(_), Nil) :: Nil if matches == cart.toSet =>

          results

        case Nil | (Root(_), Nil) :: Nil =>

          throw new UnmatchedItemsException(rules, cart.toSet.diff(matches))

        /** No more rules left to evaluate at this position in the path. */
        case (Leaf(_, Nil), Nil) :: tail =>

          val path: List[Rule] = pathFor(stack)
          val result: Map[Rule, Int] = path.foldLeft(Map.empty[Rule, Int]) { case (a, rule) =>
            a.alter(rule)(_.map(_ + 1).orElse(Some(1)))
          }

          build(tail, matches, results + result)

        case (_, Nil) :: tail =>

          build(tail, matches, results)

        /** Evaluate the next in line rule for this node's concerns. */
        case (root, rh :: rt) :: tail =>

          val mask = rh.SKUs.counted
          root.concerns.masked(mask, _.SKU) match {

            /** This node has no concerns, and is therefore a leaf. Don't descend further, and stop evaluating rules. */
            case MaskedEmpty =>
              build((root, Nil) :: tail, matches, results)

            /** This rule was entirely inapplicable. Continue by offering this node with remaining rules back to the queue. */
            case MaskedNone(_) =>
              build((root, rt) :: tail, matches, results)

            /** Every concern was masked. Descend further, adding a node for this rule, include it, but don't offer to evaluate any further rules. */
            case MaskedAll(out) =>
              val child = Leaf(rh)
              build((child, Nil) ::(root, rt) :: tail, matches ++ out, results)

            /** A few concerns remain for this node. Descend further, adding a node, include it, and offer to evaluate all rules again for remaining concerns. */
            case MaskedSome(in, out) =>
              val child = Internal(rh, in)
              build((child, rules) ::(root, rt) :: tail, matches ++ out, results)

          }

      }

    build(List(Root(cart) -> rules), Set.empty, Set.empty)

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
