package boldradius.catalog

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import boldradius.catalog.AkkaPricer.{Frontend, Result, Solve}
import boldradius.catalog.bundling.Rule
import boldradius.scala.collection._
import com.typesafe.scalalogging.LazyLogging
import squants.market.Money

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class AkkaPricer(context: ActorRefFactory)
  extends Pricer
          with LazyLogging {

  import context.dispatcher

  val frontend = context.actorOf(Props(new Frontend))

  implicit def timeout = Timeout(5 seconds)

  override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
    for {
      result <- (frontend ? Solve(rules, items)).mapTo[Result]
      costs = result.costs
    } yield costs.min

}

object AkkaPricer
  extends LazyLogging {

  case class Solution(path: List[Rule] /*, matches: Set[Item]*/) {

    def result: Map[Rule, Int] = path.counted

    def cost: Money =
      result
        .map({ case (rule, count) => rule.cost * count })
        .reduce(_ + _)

  }

  case class Solve(rules: List[Rule], items: List[Item], path: List[Rule] = Nil)

  case class Result(solutions: List[Solution]) {

    def costs = solutions.map(_.cost)

  }

  class Frontend
    extends Actor {

    import context.dispatcher

    implicit def timeout = Timeout(5 seconds)

    val solver = context.actorOf(Props(new Backend(self)), "solver")

    override def receive: Receive = {

      case solve: Solve =>
        solver ? solve pipeTo sender

    }

  }

  class Backend(frontend: ActorRef)
    extends Actor {

    import context.dispatcher

    implicit def timeout = Timeout(5 seconds)

    override def receive: Receive = {

      case Solve(rules, items, path) =>

        val results: List[Future[Result]] =
          rules map { rule =>

            val mask = rule.SKUs.counted
            items.masked(mask, _.SKU) match {

              case MaskedAll(out) =>
                Future(Result(Solution(path :+ rule) :: Nil))

              case MaskedSome(in, out) =>
                (frontend ? Solve(rules, in, path :+ rule)).mapTo[Result]

              case _ =>
                Future(Result(Nil))

            }
          }

        val result =
          for {
            r <- Future.sequence(results)
            solutions = r.flatMap(_.solutions)
          } yield Result(solutions)

        result pipeTo sender

    }

  }

}
