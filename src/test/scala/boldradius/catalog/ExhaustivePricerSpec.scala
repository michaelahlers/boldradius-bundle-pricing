package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging

class ExhaustivePricerSpec
  extends PricerSpec
          with LazyLogging {

  override def pricer: Pricer = new ExhaustivePricer

  //"Solve before the heat death of the universe" when {
  //
  //  import squants.market.USD
  //
  //  "worst-case scenario (all items, all rules)" in {
  //    val items = List.fill(20)(Inventory.all).flatten
  //    val rules = Rules.all
  //    pricer(rules, items).futureValue should be(USD(27.00))
  //  }
  //
  //}

}
