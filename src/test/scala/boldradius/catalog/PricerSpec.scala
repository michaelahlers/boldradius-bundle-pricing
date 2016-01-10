package boldradius.catalog

import boldradius.catalog.bundling.Rule
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import squants.market.USD

trait PricerSpec
  extends WordSpec
          with Matchers
          with ScalaFutures
          with LazyLogging {

  def pricer: Pricer

  object Apple extends Item(SKU = "APPLE")

  object Bread extends Item(SKU = "BREAD")

  object Celery extends Item(SKU = "CELERY")

  object Margarine extends Item(SKU = "MARGARINE")

  val A = Rule(USD(1.99), Apple)
  val B = Rule(USD(3.00), Bread)
  val M = Rule(USD(2.50), Margarine)

  val AA = Rule(USD(2.15), Apple, Apple)
  val AB = Rule(USD(1.75), Apple, Bread)

  val AAA = Rule(USD(2.00), Apple, Apple, Apple)
  val BMM = Rule(B.cost + M.cost, Bread, Margarine, Margarine)

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

}
