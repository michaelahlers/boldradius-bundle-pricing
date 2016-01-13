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

  "Matcher" must {

    "identify rule occurrences among items" in {
      val items = List(Apple, Apple, Apple, Apple, Bread, Bread, Bread)
      val rules = List(A, B, AA, AB, BB, AAA)

      val expected =
        Map(
          A -> 4,
          B -> 3,
          AA -> 2,
          AB -> 3,
          BB -> 1,
          AAA -> 1
        )

      val (_, actual) = ChocoPricer.matcher(rules, items)

      actual should be(expected)
    }

    "identify non-matching items" in {
      val items = List(Apple, Daikon, Apple, Apple, Apple, Bread, Bread, Bread, Celery)
      val rules = List(A, B, AA, AB, BB, AAA)

      val expected = Set(Celery, Daikon)
      val (actual, _) = ChocoPricer.matcher(rules, items)

      actual should be(expected)
    }

  }

}
