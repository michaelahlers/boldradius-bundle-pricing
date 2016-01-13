package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging

class ExhaustivePricerSpec
  extends PricerSpec
          with LazyLogging {

  override def pricer: Pricer = new ExhaustivePricer

}
