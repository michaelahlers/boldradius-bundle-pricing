package boldradius.catalog

import boldradius.catalog.Pricer.UnmatchedItemsException
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import squants.market.USD

import scala.concurrent.Await
import scala.language.postfixOps

trait PricerSpec
  extends WordSpec
          with Matchers
          with ScalaFutures
          with LazyLogging {

  def pricer: Pricer

  object Apple extends Item(SKU = "APPLE")

  object Bread extends Item(SKU = "BREAD")

  object Celery extends Item(SKU = "CELERY")

  object Daikon extends Item(SKU = "DAIKON")

  object Eggplant extends Item(SKU = "EGGPLANT")

  object Figs extends Item(SKU = "FIGS")

  object Grapes extends Item(SKU = "GRAPES")

  object Margarine extends Item(SKU = "MARGARINE")

  object Inventory {

    val all =
      Apple ::
        Bread ::
        Celery ::
        Daikon ::
        Eggplant ::
        Figs ::
        Grapes ::
        Margarine ::
        Nil

  }

  val A = Rule(USD(1.99), Apple)
  val B = Rule(USD(3.00), Bread)
  val C = Rule(USD(2.00), Celery)
  val D = Rule(USD(1.00), Daikon)
  val E = Rule(USD(2.50), Eggplant)
  val F = Rule(USD(2.00), Figs)
  val G = Rule(USD(5.00), Grapes)
  val M = Rule(USD(2.50), Margarine)

  val AA = Rule(USD(2.15), Apple, Apple)
  val AB = Rule(USD(1.75), Apple, Bread)
  val AC = Rule(USD(1.75), Apple, Celery)
  val AD = Rule(USD(1.75), Apple, Daikon)
  val AE = Rule(USD(1.75), Apple, Eggplant)
  val AF = Rule(USD(1.75), Apple, Figs)
  val AG = Rule(USD(1.75), Apple, Grapes)

  val BB = Rule(USD(5.00), Bread, Bread)

  val AAA = Rule(USD(2.00), Apple, Apple, Apple)
  val AAB = Rule(USD(2.00), Apple, Apple, Bread)
  val ABB = Rule(USD(2.00), Apple, Bread, Bread)

  val BMM = Rule(B.cost + M.cost, Bread, Margarine, Margarine)

  object Rules {

    val all =
      A ::
        B ::
        C ::
        D ::
        E ::
        F ::
        G ::
        M ::
        AA ::
        AB ::
        AC ::
        AD ::
        AE ::
        AF ::
        AG ::
        BB ::
        AAA ::
        AAB ::
        ABB ::
        BMM ::
        Nil

  }

  "Return priced shopping carts" when {

    "a cart is provided" in {
      import scala.concurrent.ExecutionContext.Implicits.global

      val rules = List(A, B)
      val cart = Cart(Apple, Apple, Bread)
      val expected = PricedCart(USD(6.98), Apple, Apple, Bread)
      pricer(rules, cart).futureValue should be(expected)
    }

  }

  "Apples sold with a group discount" when {

    val rules = List(A, AA)

    "an apple is bought" must {
      val expected = USD(1.99)
      s"cost $expected" in {
        pricer(rules, Apple).futureValue should be(expected)
      }
    }

    "two apples are bought" must {
      val expected = USD(2.15)
      s"cost $expected" in {
        pricer(rules, Apple, Apple).futureValue should be(expected)
      }
    }

  }

  "Bread and margarine sold with a group discount" when {

    val rules = List(B, M, BMM)

    "a loaf is bought alone" must {
      val expected = USD(3.0)
      s"cost $expected" in {
        pricer(rules, Bread).futureValue should be(expected)
      }
    }

    "a stick of margarine is bought alone" must {
      val expected = USD(2.50)
      s"cost $expected" in {
        pricer(rules, Margarine).futureValue should be(expected)
      }
    }

    "a loaf of bread and stick of margarine bought together" must {
      val expected = USD(5.50)
      s"cost $expected" in {
        pricer(rules, Bread, Margarine).futureValue should be(expected)
      }
    }

    "a loaf of bread and two sticks of margarine bought together" must {
      val expected = USD(5.50)
      s"cost $expected" in {
        pricer(rules, Bread, Margarine, Margarine).futureValue should be(expected)
      }
    }

    "two loafs of bread and three sticks of margarine bought together" must {
      val expected = USD(11.00)
      s"cost $expected" in {
        pricer(rules, Bread, Bread, Margarine, Margarine, Margarine).futureValue should be(expected)
      }
    }

  }

  info("It's possible for a less expensive bundle price to inadvertently cause a higher price by excluding other more expensive bundles that may result in a lower net price.")

  "Three apples and a loaf of bread" when {

    val items = List(Apple, Apple, Apple, Bread)

    "priced independently" must {
      val rules = List(A, B)
      val expected = USD(8.97)
      s"cost $expected" in {
        pricer(rules, items).futureValue should be(expected)
      }
    }

    "priced with two apples and an apple and bread discounts" must {
      val rules = List(AA, AB)
      val expected = USD(3.90)
      s"cost $expected" in {
        pricer(rules, items).futureValue should be(expected)
      }
    }

    "priced with a three apple discount" must {
      val rules = List(AAA, B)
      val expected = USD(5.00)
      s"cost $expected" in {
        pricer(rules, items).futureValue should be(expected)
      }
    }

    "priced with all overlapping group discounts" must {
      val rules = List(A, B, AA, AB, AAA)
      val expected = USD(3.90)
      s"cost $expected" in {
        pricer(rules, items).futureValue should be(expected)
      }
    }

  }

  "Pricing carts" must {

    "report failure" when {

      import scala.concurrent.duration._

      /* TODO: Figure out why ScalaFutures.futureValue won't produce the correct exception. */
      def awaitPricer(rules: List[Rule], items: List[Item], timeout: Duration = 1 second) =
        Await.result(pricer(rules, items), timeout)

      "no rules are given for items" in {
        val items = List(Apple, Apple, Apple, Bread, Celery)
        val rules = List.empty[Rule]

        val thrown = the[UnmatchedItemsException] thrownBy awaitPricer(rules, items)
        // thrown.rules should be(rules)
        thrown.items should be(items.toSet)
      }

      "any individual items can't match" in {
        val items = List(Apple, Apple, Apple, Bread, Celery)
        val rules = List(AA, B)

        val thrown = the[UnmatchedItemsException] thrownBy awaitPricer(rules, items)
        // thrown.rules should be(rules)
        thrown.items should be(Set(Celery))
      }

      "an item is grouped but can't match because of missing but accounted for item" in {
        val items = List(Bread, Celery)
        val rules = List(A, AB, C)

        val thrown = the[UnmatchedItemsException] thrownBy awaitPricer(rules, items)
        // thrown.rules should be(rules)
        thrown.items should be(Set(Bread))
      }

    }

  }

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
