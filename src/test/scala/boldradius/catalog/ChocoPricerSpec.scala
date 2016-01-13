package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging

import scala.language.postfixOps

class ChocoPricerSpec
  extends PricerSpec
          with LazyLogging {

  override def pricer: Pricer = new ChocoPricer

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
