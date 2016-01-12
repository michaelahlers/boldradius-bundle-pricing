package boldradius.catalog

import boldradius.catalog.Pricer.UnmatchedItemsException
import boldradius.scala.collection._
import boldradius.syntax.scala.collection._
import com.typesafe.scalalogging.LazyLogging
import squants.market.Money

import scala.annotation.tailrec
import scala.concurrent.Future

class SimplePricer
  extends Pricer
          with LazyLogging {

  private sealed trait Node {
    def concerns: List[Item]
  }

  private case class Root(concerns: List[Item]) extends Node

  private case class Internal(rule: Rule, concerns: List[Item], root: Node) extends Node

  private case class Leaf(rule: Rule, concerns: List[Item] = Nil, root: Node) extends Node

  private def solver(rules: List[Rule], items: List[Item]) = {

    def childrenWithMatchesFor(root: Node): (List[Node], Set[Item]) = {

      val results: List[Option[(Node, List[Item])]] =
        rules map { rule =>
          val mask = rule.SKUs.counted
          root.concerns.masked(mask, _.SKU) match {
            case MaskedAll(out) =>
              Some(Leaf(rule, Nil, root) -> out)
            case MaskedSome(in, out) =>
              Some(Internal(rule, in, root) -> out)
            case _ =>
              None
          }
        }

      val (children, matches) =
        results.flatten.foldLeft((List.empty[Node], List.empty[Item])) {
          case ((nodes, allMatches), (node, nodeMatches)) =>
            (nodes :+ node, allMatches ++ nodeMatches)
        }

      children -> matches.toSet

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
        case Nil if matches == items.toSet =>

          results

        case Nil =>

          throw new UnmatchedItemsException(rules, items.toSet.diff(matches))

        /** No more rules left to evaluate at this position in the path. */
        case Leaf(rule, concerns, root) :: tail =>

          val path: List[Rule] = pathFor(Leaf(rule, concerns, root), Nil)
          val result: Map[Rule, Int] = path.counted

          build(tail, matches, results + result)

        case root :: tail =>

          val (children, localMatches) = childrenWithMatchesFor(root)
          build(children ++ tail, matches ++ localMatches, results)

      }

    val (children, localMatches) = childrenWithMatchesFor(Root(items))
    build(children, localMatches, Set.empty)

  }

  /** @inheritdoc*/
  override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
    Future.successful {
      val costs =
        solver(rules, items) map { solution =>
          solution
            .map({ case (rule, count) => rule.cost * count })
            .reduce(_ + _)
        }

      costs.min
    }

}
