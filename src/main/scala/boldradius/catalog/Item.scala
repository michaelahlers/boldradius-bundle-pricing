package boldradius.catalog

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
