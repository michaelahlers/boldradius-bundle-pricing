package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging

class SimplePricerSpec
  extends PricerSpec
          with LazyLogging {

  override def pricer: Pricer = new SimplePricer

}
