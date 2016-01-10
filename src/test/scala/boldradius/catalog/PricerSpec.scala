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

  val BMM = Rule(B.cost + M.cost, Bread, Margarine, Margarine)

  "Apples sold with a group discount" when {

    val rules = List(A, AA)

    "an apple is bought=" must {
      s"cost ${A.cost}" in {
        pricer(rules, Apple).futureValue should be(A.cost)
      }
    }

    "two apples are bought" must {
      s"cost ${AA.cost}" in {
        pricer(rules, Apple, Apple).futureValue should be(AA.cost)
      }
    }

  }

  "Bread and margarine sold with a group discount" when {

    val rules = List(B, M, BMM)

    "a loaf is bought alone" must {
      s"cost ${B.cost}" in {
        pricer(rules, Bread).futureValue should be(B.cost)
      }
    }

    "a stick of margarine is bought alone" must {
      s"cost ${M.cost}" in {
        pricer(rules, Margarine).futureValue should be(M.cost)
      }
    }

    "a loaf of bread and stick of margarine bought together" must {
      s"cost ${B.cost + M.cost}" in {
        pricer(rules, Bread, Margarine).futureValue should be(B.cost + M.cost)
      }
    }

    "a loaf of bread and two sticks of margarine bought together" must {
      s"cost ${BMM.cost}" in {
        pricer(rules, Bread, Margarine, Margarine).futureValue should be(BMM.cost)
      }
    }

    "two loafs of bread and three sticks of margarine bought together" must {
      s"cost ${B.cost + BMM.cost + M.cost}" in {
        pricer(rules, Bread, Bread, Margarine, Margarine, Margarine).futureValue should be(B.cost + BMM.cost + M.cost)
      }
    }

  }

}
