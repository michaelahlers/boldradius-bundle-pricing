package boldradius.catalog

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import boldradius.catalog.AkkaPricer.{Result, Solve, Solver}
import boldradius.catalog.Pricer.UnmatchedItemsException
import boldradius.catalog.bundling.Rule
import boldradius.scala.collection._
import com.typesafe.scalalogging.LazyLogging
import squants.Money

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class AkkaPricer(context: ActorRefFactory)
  extends Pricer
          with LazyLogging {

  import context.dispatcher

  val router =
    context.actorOf(Props(new Actor {

      val solvers = (0 until 5).map(_ => context.actorOf(Props(new Solver(self))))

      var index = 0

      override def receive: Receive = {
        case message =>
          val solver = solvers(index)
          solver forward message
          index = (index + 1) % solvers.size
      }

    }))

  implicit def timeout = Timeout(5 seconds)

  override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
    (router ? Solve(rules, items)).mapTo[Result] map { result =>
      result.assertFinished(rules, items)
      result.costs.min
    }

}

object AkkaPricer
  extends LazyLogging {

  case class Path(rules: List[Rule]) {

    def cost: Money =
      rules
        .counted
        .map({ case (rule, count) => rule.cost * count })
        .reduce(_ + _)

    def ++:(foo: Traversable[Rule]) = copy(rules = foo ++: rules)

    def nonEmpty: Boolean = rules.nonEmpty

  }

  object Path {
    def apply(rule: Rule): Path = Path(rule :: Nil)
  }

  case class Solve(rules: List[Rule], items: List[Item], aggregator: Option[ActorRef])

  object Solve {

    def apply(rules: List[Rule], items: List[Item], aggregator: ActorRef): Solve =
      Solve(rules, items, Some(aggregator))

    def apply(rules: List[Rule], items: List[Item]): Solve =
      Solve(rules, items, None)

  }

  case class Result(paths: List[Path], matches: Set[Item]) {

    def costs = paths.map(_.cost)

    def assertFinished(rules: List[Rule], items: List[Item]): Unit = {
      val difference = items.toSet.diff(matches)
      if (difference.nonEmpty) throw UnmatchedItemsException(rules, difference)
    }


  }

  class Aggregator(expectedRules: List[Rule], head: Option[Rule], replyTo: ActorRef)
    extends Actor {

    val matches: mutable.Buffer[Item] = mutable.Buffer.empty

    val paths: mutable.Buffer[Path] = mutable.Buffer.empty

    val rules: mutable.Buffer[Rule] = mutable.Buffer.empty

    def reply() =
      if (expectedRules.size == rules.size) {
        replyTo ! Result(paths.filter(_.nonEmpty).map(head ++: _).toList, matches.toSet)
        head.foreach(replyTo !)
      }

    reply()

    override def receive: Receive = {

      case item: Item =>
        matches += item

      case path: Path =>
        paths += path

      case rule: Rule =>
        rules += rule
        reply()

      case result: Result =>
        paths ++= result.paths

    }

  }

  object Aggregator {

    def props(rules: List[Rule], rule: Rule, replyTo: ActorRef): Props =
      Props(new Aggregator(rules, Some(rule), replyTo))

    def props(rules: List[Rule], replyTo: ActorRef): Props =
      Props(new Aggregator(rules, None, replyTo))

  }

  class Solver(router: ActorRef)
    extends Actor {

    override def receive: Receive = {

      case Solve(rules, items, None) =>
        router ! Solve(rules, items, Some(context.actorOf(Aggregator.props(rules, sender))))

      case Solve(rules, items, Some(aggregator)) =>
        rules foreach { rule =>
          val mask = rule.SKUs.counted

          items.masked(mask, _.SKU) match {

            case MaskedAll(out) =>
              out.foreach(aggregator !)
              aggregator ! Path(rule)
              aggregator ! rule

            case MaskedSome(in, out) =>
              out.foreach(aggregator !)
              router ! Solve(rules, in, context.actorOf(Aggregator.props(rules, rule, aggregator)))

            case _ =>
              aggregator ! Path(Nil)
              aggregator ! rule

          }
        }

    }

  }

}
