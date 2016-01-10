package boldradius.catalog

import boldradius.squants.json.MoneyFormat
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Reads, _}
import shapeless.syntax.std.tuple._
import squants.Money

/**
 * Abstraction for a collection of [[Item items]], which may or may not yet have a [[squants.Money cost]].
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
   * Establishes a new [[squants.Money cost]] for the cart, producing a [[PricedCart]].
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

  implicit val reads: Reads[Cart] =
    UnpricedCart.reads or
      PricedCart.reads

  implicit val writes: Writes[Cart] =
    Writes {
      case c: UnpricedCart => UnpricedCart.writes.writes(c)
      case c: PricedCart => PricedCart.writes.writes(c)
    }

}

case class UnpricedCart(items: List[Item]) extends Cart {

  def withItems(items: List[Item]): UnpricedCart = copy(items = items)

  override def withCost(cost: Money): PricedCart = PricedCart(items, cost)

}

object UnpricedCart {

  def apply(item: Item): UnpricedCart = UnpricedCart(items = item :: Nil)

  val reads: Reads[Cart] = (
    (__ \ 'items).read[List[Item]] and
      (__ \ '_type).read[String](pattern("unpriced".r))
    ) ({ (items, _) => UnpricedCart(items) })

  val writes: OWrites[UnpricedCart] = (
    (__ \ 'items).write[List[Item]] and
      (__ \ '_type).write[String]
    ) (_ :+ "unpriced")

}

case class PricedCart(items: List[Item], cost: Money) extends Cart {

  def withItems(items: List[Item]): UnpricedCart = UnpricedCart(items = items)

  override def withCost(cost: Money): PricedCart = PricedCart(items, cost)

}

object PricedCart {

  def apply(item: Item, cost: Money): PricedCart = PricedCart(items = item :: Nil, cost = cost)

  val reads: Reads[Cart] = (
    (__ \ 'items).read[List[Item]] and
      (__ \ 'cost).read[Money] and
      (__ \ '_type).read[String](pattern("priced".r))
    ) ({ (items, cost, _) => PricedCart(items, cost) })

  val writes: OWrites[PricedCart] = (
    (__ \ 'items).write[List[Item]] and
      (__ \ 'cost).write[Money] and
      (__ \ '_type).write[String]
    ) (_ :+ "priced")

}
