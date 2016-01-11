package boldradius.catalog

import boldradius.catalog.bundling.Rule
import boldradius.scala.collection.{MaskedAll, MaskedSome, _}
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

  case class Internal(rule: Rule, concerns: List[Item], root: Node) extends Node

  case class Leaf(rule: Rule, concerns: List[Item] = Nil, root: Node) extends Node

  def materialize(rules: List[Rule], cart: List[Item]) = {

    def childrenFor(root: Node): List[Node] = {
      val children: List[Option[Node]] =
        rules map { rule =>
          val mask = rule.SKUs.counted
          root.concerns.masked(mask, _.SKU) match {
            case MaskedAll(_) =>
              Some(Leaf(rule, Nil, root))
            case MaskedSome(in, _) =>
              Some(Internal(rule, in, root))
            case _ =>
              None
          }
        }

      children.flatten
    }

    @tailrec
    def pathFor(node: Node, path: List[Rule]): List[Rule] =
      node match {
        case Root(_) => path
        case Internal(rule, _, root) => pathFor(root, rule +: path)
        case Leaf(rule, _, root) => pathFor(root, rule +: path)
      }

    @tailrec
    def build(queue: List[Node], matches: Set[Item], results: Set[Map[Rule, Int]]): Set[Map[Rule, Int]] =
      queue match {

        /** Arriving at the root indicates all matches have been explored. */
        case Nil =>

          results

        //case Nil | (Root(_), Nil) :: Nil =>
        //
        //  throw new UnmatchedItemsException(rules, cart.toSet.diff(matches))

        /** No more rules left to evaluate at this position in the path. */
        case Leaf(rule, concerns, root) :: tail =>

          val path: List[Rule] = pathFor(Leaf(rule, concerns, root), Nil)
          val result: Map[Rule, Int] = path.foldLeft(Map.empty[Rule, Int]) { case (a, rule) =>
            a.alter(rule)(_.map(_ + 1).orElse(Some(1)))
          }

          build(tail, matches, results + result)

        case root :: tail =>

          build(childrenFor(root) ++ tail, matches, results)

      }

    build(childrenFor(Root(cart)), Set.empty, Set.empty)

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
