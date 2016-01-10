package boldradius.catalog

import squants.Money

/**
 * Abstraction for a collection of [[Item items]], which may or may not yet have a [[Money cost]].
 */
sealed trait Cart {

  /**
   * All items in the cart.
   */
  def items: List[Item]

  /**
   * Replace contents with the given [[Item items]], always producing a [[UnpricedCart]].
   */
  def withItems(items: List[Item]): UnpricedCart

  final def addItems(extraItems: List[Item]): UnpricedCart = withItems(items = items ++ extraItems)

  final def addItem(extraItem: Item): UnpricedCart = withItems(items = items :+ extraItem)

  /**
   * Establishes a new [[Money cost]] for the cart, producing a [[PricedCart]].
   */
  def withCost(cost: Money): PricedCart

}

object Cart {

  /**
   * @see [[UnpricedCart]]
   */
  def apply(items: List[Item]): UnpricedCart = UnpricedCart(items = items)

  /**
   * @see [[UnpricedCart]]
   */
  def apply(item: Item): UnpricedCart = UnpricedCart(items = item :: Nil)

  /**
   * @see [[PricedCart]]
   */
  def apply(items: List[Item], cost: Money): PricedCart = PricedCart(items = items, cost = cost)

  /**
   * @see [[PricedCart]]
   */
  def apply(item: Item, cost: Money): PricedCart = PricedCart(items = item :: Nil, cost = cost)

}

case class UnpricedCart(items: List[Item]) extends Cart {

  def withItems(items: List[Item]): UnpricedCart = copy(items = items)

  override def withCost(cost: Money): PricedCart = PricedCart(items, cost)

}

object UnpricedCart {

  def apply(item: Item): UnpricedCart = UnpricedCart(items = item :: Nil)

}

case class PricedCart(items: List[Item], cost: Money) extends Cart {

  def withItems(items: List[Item]): UnpricedCart = UnpricedCart(items = items)

  override def withCost(cost: Money): PricedCart = PricedCart(items, cost)

}

object PricedCart {

  def apply(item: Item, cost: Money): PricedCart = PricedCart(items = item :: Nil, cost = cost)

}
