package boldradius.catalog

import boldradius.scala.collection.{MaskedAll, MaskedSome}
import boldradius.syntax.scala.collection._
import com.typesafe.scalalogging.LazyLogging
import squants.market.Money

import scala.annotation.tailrec
import scala.concurrent.Future
import scalaz.Scalaz._

class ChocoPricer
  extends Pricer
          with LazyLogging {

  override def apply(rules: List[Rule], items: List[Item]): Future[Money] = ???

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
