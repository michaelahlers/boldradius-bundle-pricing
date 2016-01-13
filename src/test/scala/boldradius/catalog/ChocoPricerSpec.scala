package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging

//import squants.market.USD

//import scala.concurrent.Await
//import scala.concurrent.duration._

class ChocoPricerSpec
  extends PricerSpec
          with LazyLogging {

  override def pricer: Pricer = new ChocoPricer

  //  "Solve before the heat death of the universe" when {
  //
  //    def awaitPricer(rules: List[Rule], items: List[Item], timeout: Duration = 60 seconds) =
  //      Await.result(pricer(rules, items), timeout)
  //
  //    "worst-case scenario (all items, all rules)" in {
  //      val items = List.fill(2)(Inventory.all).flatten
  //       val items = List.fill(100)(Apple)
  //      val rules = Rules.all
  //      awaitPricer(rules, items) should be(USD(27.00))
  //    }
  //
  //  }

}
