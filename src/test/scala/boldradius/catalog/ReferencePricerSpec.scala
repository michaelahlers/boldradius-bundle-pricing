package boldradius.catalog

import boldradius.catalog.bundling.Rule
import com.typesafe.scalalogging.LazyLogging
import squants.market.{Money, USD}

import scala.concurrent.Future

/**
 * Validates [[PricerSpec]] test fixtures for correctness.
 */
class ReferencePricerSpec
  extends PricerSpec
          with LazyLogging {

  override def pricer: Pricer =
    new Pricer {

      override def apply(rules: List[Rule], items: List[Item]): Future[Money] =
        Future.successful {
          rules match {

            case List(A, AA) =>
              items match {
                case List(Apple) =>
                  USD(1.99)
                case Apple :: Apple :: Nil =>
                  USD(2.15)
              }

            case List(B, M, BMM) =>
              items match {
                case List(Bread) =>
                  USD(3.00)
                case List(Margarine) =>
                  USD(2.50)
                case List(Bread, Margarine) | List(Bread, Margarine, Margarine) =>
                  USD(5.50)
                case List(Bread, Bread, Margarine, Margarine, Margarine) =>
                  USD(11.00)
              }

          }
        }

    }

}
