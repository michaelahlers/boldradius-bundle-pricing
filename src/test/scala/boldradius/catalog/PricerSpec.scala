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

  object items {

    val apple = Item(SKU = "APPLE")

    val bread = Item(SKU = "BREAD")

    val celery = Item(SKU = "CELERY")

    val margarine = Item(SKU = "MARGARINE")

  }

  "Apples sold with a group discount" when {

    import items.apple

    val A = Rule(USD(1.99), apple)
    val AA = Rule(USD(2.15), apple, apple)

    val rules = List(A, AA)

    "an apple is bought=" must {
      s"cost ${A.cost}" in {
        pricer(rules, apple).futureValue should be(A.cost)
      }
    }

    "two apples are bought" must {
      s"cost ${AA.cost}" in {
        pricer(rules, apple, apple).futureValue should be(AA.cost)
      }
    }

  }

  "Bread and margarine sold with a group discount" when {

    import items.{bread, margarine}

    val B = Rule(USD(3.00), bread)
    val M = Rule(USD(2.50), margarine)
    val BMM = Rule(B.cost + M.cost, bread, margarine, margarine)

    val rules = List(B, M, BMM)

    "a loaf is bought alone" must {
      s"cost ${B.cost}" in {
        pricer(rules, bread).futureValue should be(B.cost)
      }
    }

    "a stick of margarine is bought alone" must {
      s"cost ${M.cost}" in {
        pricer(rules, margarine).futureValue should be(M.cost)
      }
    }

    "a loaf of bread and stick of margarine bought together" must {
      s"cost ${B.cost + M.cost}" in {
        pricer(rules, bread, margarine).futureValue should be(B.cost + M.cost)
      }
    }

    "a loaf of bread and two sticks of margarine bought together" must {
      s"cost ${BMM.cost}" in {
        pricer(rules, bread, margarine, margarine).futureValue should be(BMM.cost)
      }
    }

    "two loafs of bread and three sticks of margarine bought together" must {
      s"cost ${B.cost + BMM.cost + M.cost}" in {
        pricer(rules, bread, bread, margarine, margarine, margarine).futureValue should be(B.cost + BMM.cost + M.cost)
      }
    }

  }

}
