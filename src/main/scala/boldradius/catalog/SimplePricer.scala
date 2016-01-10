package boldradius.catalog

import boldradius.catalog.bundling.Rule
import com.typesafe.scalalogging.LazyLogging
import squants.market.{Money, USD}

import scala.concurrent.Future

class SimplePricer
  extends Pricer
          with LazyLogging {

  override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
    Future.successful {
      USD(1)
    }

}
