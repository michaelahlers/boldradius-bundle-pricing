package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging
import squants.market.Money

import scala.concurrent.Future

class ConstraintPricer
  extends Pricer
          with LazyLogging {

  override def apply(rules: List[Rule], items: List[Item]): Future[Money] = ???

}

object ConstraintPricer {


}
