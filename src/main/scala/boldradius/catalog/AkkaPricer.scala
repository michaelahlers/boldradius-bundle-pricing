package boldradius.catalog

import akka.actor._
import akka.pattern.ask
import akka.routing.RoundRobinPool
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

/**
 * A [[Pricer]] built on Akka that allows the work load of “solving” a price to be distributed across arbitrary actors (including those running remotely).
 *
 * @param context Any kind of factory (_e.g._, [[akka.actor.ActorSystem]], [[akka.actor.ActorContext]]) wherein this implementation will make its [[akka.actor.Actor workers]].
 */
class AkkaPricer(context: ActorRefFactory)
  extends Pricer
          with LazyLogging {

  import context.dispatcher

  /** Play-pretend router. */
  val router = context.actorOf(RoundRobinPool(5).props(Props[Solver]))

  implicit def timeout = Timeout(5 seconds)

  /** @inheritdoc*/
  override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
    (router ? Solve(rules, items)).mapTo[Result] map { result =>
      result.assertFinished(rules, items)
      result.costs.min
    }

}

object AkkaPricer
  extends LazyLogging {

  /**
   * Represents matching rules as items are consumed.
   */
  private case class Path(rules: List[Rule]) {

    def cost: Money =
      rules
        .counted
        .map({ case (rule, count) => rule.cost * count })
        .reduce(_ + _)

    /** Prepends this [[Path]] with the given rules. */
    def ++:(precedingRules: Traversable[Rule]) = copy(rules = precedingRules ++: rules)

  }

  private object Path {
    def apply(rule: Rule): Path = Path(rule :: Nil)
  }

  /**
   * @param rules An authoritative set of rules.
   * @param items Items to be evaluated (generally decreases over time).
   * @param aggregator Accumulates solutions.
   */
  private case class Solve(rules: List[Rule], items: List[Item], aggregator: Option[ActorRef])

  private object Solve {

    def apply(rules: List[Rule], items: List[Item], aggregator: ActorRef): Solve =
      Solve(rules, items, Some(aggregator))

    def apply(rules: List[Rule], items: List[Item]): Solve =
      Solve(rules, items, None)

  }

  /**
   * @param paths Accumulated paths at any given point in the solution hierarchy.
   * @param matches All items that've positively matched rules.
   */
  private case class Result(paths: List[Path], matches: Set[Item]) {

    def costs = paths.map(_.cost)

    def assertFinished(rules: List[Rule], items: List[Item]): Unit = {
      val difference = items.toSet.diff(matches)
      if (difference.nonEmpty) throw UnmatchedItemsException(rules, difference)
    }

  }

  /**
   * While [[Solver]] performs the computation, this actor accumulates results.
   *
   * @param expectedRules All rules which must be evaluated before work is considered complete.
   * @param head An optional head [[Rule]], which, if present, indicates an internal element of a [[Path]], or, if absent, represents the terminator.
   * @param replyTo An actor to receive a [[Result]] when finished, and, if an internal part of the path, the root [[Rule]].
   */
  private class Aggregator(expectedRules: List[Rule], head: Option[Rule], replyTo: ActorRef)
    extends Actor {

    /** [[Item]] objects which have positively matched a rule. */
    val matches: mutable.Buffer[Item] = mutable.Buffer.empty

    val paths: mutable.Buffer[Path] = mutable.Buffer.empty

    /** [[Rule]] objects which have been evaluated (used to determine when this aggregation is done). */
    val rules: mutable.Buffer[Rule] = mutable.Buffer.empty

    /** Considers whether all expected rules have been evaluated. If they have, replies with a [[Result]] and, if appropriate, its own [[Rule]] ot signal evaluation. */
    def reply() =
      if (expectedRules.size == rules.size) {
        replyTo ! Result(paths.map(head ++: _).toList, matches.toSet)
        head.foreach(replyTo !)
      }

    reply()

    /**
     * Generally accumulates its messages except in two cases: 1) a [[Rule]] is received, and [[reply]] is called, and 2) a [[Result]], originating from another [[Aggregator]], whose paths are added to those already known.
     */
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

  private object Aggregator {

    def props(rules: List[Rule], rule: Rule, replyTo: ActorRef): Props =
      Props(new Aggregator(rules, Some(rule), replyTo))

    def props(rules: List[Rule], replyTo: ActorRef): Props =
      Props(new Aggregator(rules, None, replyTo))

  }

  /**
   * While these don't contain mutable state, they _do_ serve as message endpoints (namely for [[Solve]]).
   */
  private class Solver
    extends Actor {

    override def receive: Receive = {

      /** The [[sender]] wishes to receive the [[Result]]. */
      case Solve(rules, items, None) =>

        /* Don't pass back up, and possibly incur network cost. */
        self ! Solve(rules, items, Some(context.actorOf(Aggregator.props(rules, sender))))

      /** Given [[Aggregator]] will be signaled with evaluations. */
      case Solve(rules, items, Some(aggregator)) =>

        /* Breadth-first traversal. */
        rules foreach { rule =>

          val mask = rule.SKUs.counted
          items.masked(mask, _.SKU) match {

            /** Every [[Item]] got matched. Pass them all to the [[Aggregator]], including a [[Path]] (which, in this case, is a leaf), and signal the [[Rule]] was evalated. */
            case MaskedAll(out) =>
              out.foreach(aggregator !)
              aggregator ! Path(rule)
              aggregator ! rule

            /** A subset was masked, therefore there's further descending to do. Signal the incoming [[Aggregator]] which items have been matched, then create a new [[Aggregator]] to descend further. It will be responsible for signalling the [[Rule]] was evaluated. */
            case MaskedSome(in, out) =>
              out.foreach(aggregator !)
              context.parent ! Solve(rules, in, context.actorOf(Aggregator.props(rules, rule, aggregator)))

            /** Either [[MaskedEmpty]] or [[MaskedNone]]. In this case, signal the aggregator that the [[Rule]] was evaluated, but there's no sense providing it a [[Path]]. */
            case _ =>
              aggregator ! rule

          }

        }

    }

  }

}
