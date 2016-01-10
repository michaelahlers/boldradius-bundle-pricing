package boldradius.catalog

class SimplePricerSpec
  extends PricerSpec {

  override def pricer: Pricer = new SimplePricer

}
