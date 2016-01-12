package boldradius.catalog

import boldradius.catalog.bundling.Rule
import squants.market.Money

import scala.concurrent.Future

/**
 * A (typical) [[Pricer]] implementation exhaustively searches for the lowest price for a shopping cart, given rules which may provide discounts under certain circumstances.
 */
trait Pricer {

  final def apply(rules: List[Rule], cart: Cart): Future[Money] =
    apply(rules = rules, items = cart.items)

  final def apply(rules: List[Rule], items: Item*): Future[Money] =
    apply(rules = rules, items = items.toList)

  /**
   * Given the provided [[bundling.Rule rules]], calculate the lowest price for the [[Item items]].
   */
  def apply(rules: List[Rule], items: List[Item]): Future[Money]

}

object Pricer {

  case class UnmatchedItemsException(rules: List[Rule], items: Set[Item]) extends Exception {
    override def getMessage: String =
      """Items in %s didn't match any rules in %s."""
        .format(
          items.map(_.SKU).mkString("{", ", ", "}"),
          rules.map(_.SKUs.mkString("{", ", ", "}")).mkString("{", ", ", "}")
        )
  }

}
