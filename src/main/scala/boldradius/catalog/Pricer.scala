package boldradius.catalog

import boldradius.catalog.bundling.Rule
import squants.market.Money

import scala.concurrent.Future

sealed trait Pricer {

  def apply(rules: List[Rule], cart: Cart): Future[Money] =
    apply(rules = rules, items = cart.items)

  def apply(rules: List[Rule], items: Item*): Future[Money] =
    apply(rules = rules, items = items.toList)

  def apply(rules: List[Rule], items: List[Item]): Future[Money]

}
