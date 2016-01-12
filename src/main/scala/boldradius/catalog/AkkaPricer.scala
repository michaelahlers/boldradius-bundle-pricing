package boldradius.catalog

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import boldradius.catalog.AkkaPricer.{Result, Solve, Solver}
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

  val solver = context.actorOf(Props(new Solver))

  implicit def timeout = Timeout(5 seconds)

  override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
    (solver ? Solve(rules, items)).mapTo[Result] map { result =>
      result.costs.min
    }

  //(frontend ? Solve(rules, items)).mapTo[Result] map {
  //  case result if result.matches == items.toSet =>
  //    result.costs.min
  //  case result =>
  //    throw new UnmatchedItemsException(rules, items.toSet.diff(result.matches))
  //}

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

  case class Result(paths: List[Path]) {

    def costs = paths.map(_.cost)

  }

  class Aggregator(expectedRules: List[Rule], head: Option[Rule], replyTo: ActorRef)
    extends Actor {

    val paths: mutable.Buffer[Path] = mutable.Buffer.empty

    val rules: mutable.Buffer[Rule] = mutable.Buffer.empty

    override def receive: Receive = {

      case path: Path =>
        paths += path

      case rule: Rule =>
        rules += rule

        if (expectedRules.size == rules.size) {
          replyTo ! Result(paths.filter(_.nonEmpty).map(head ++: _).toList)
          head.foreach(replyTo !)
        }

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

  class Solver
    extends Actor {

    override def receive: Receive = {

      case Solve(rules, items, None) =>
        self ! Solve(rules, items, Some(context.actorOf(Aggregator.props(rules, sender))))

      case Solve(rules, items, Some(aggregator)) =>
        rules foreach { rule =>
          val mask = rule.SKUs.counted

          items.masked(mask, _.SKU) match {

            case MaskedAll(_) =>
              aggregator ! Path(rule)
              aggregator ! rule

            case MaskedSome(in, _) =>
              self ! Solve(rules, in, context.actorOf(Aggregator.props(rules, rule, aggregator)))

            case _ =>
              aggregator ! Path(Nil)
              aggregator ! rule

          }
        }

    }

  }

}
