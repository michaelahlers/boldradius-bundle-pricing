package boldradius.catalog

case class Item(
  id: ItemId = ItemId.next,
  SKU: String
)
