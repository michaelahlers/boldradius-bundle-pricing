package boldradius.catalog

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Represents an item in the shopping catalog.
 *
 * @param SKU Stock keeping unit distinctly identifies an item for sale.
 *
 * @author [[mailto:michael@ahlers.co Michael Ahlers]]
 *
 * @see [[https://en.wikipedia.org/wiki/Stock_keeping_unit Stock keeping unit]]
 */
case class Item(
  id: ItemId = ItemId.next,
  SKU: String
)

object Item {

  implicit def reads: Reads[Item] = (
    (__ \ 'id).read[Long].map(ItemId(_)) and
      (__ \ 'SKU).read[String]
    ) (Item.apply _)

  implicit def writes: OWrites[Item] = (
    (__ \ 'id).write[Long].contramap[ItemId](_.value) and
      (__ \ 'SKU).write[String]
    ) (unlift(Item.unapply))

}
