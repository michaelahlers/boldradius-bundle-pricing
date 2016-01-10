package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}
import squants.market.{JPY, USD}

class CartSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "Shopping carts" must {
    val items =
      Item(SKU = "APPLE") ::
        Item(SKU = "BREAD") ::
        Item(SKU = "MARGARINE") ::
        Nil

    "remain unpriced" when {
      "a new item is added" in {
        UnpricedCart(items.head).addItem(items.last) should be(UnpricedCart(List(items.head, items.last)))
      }

      "new items are added" in {
        UnpricedCart(items.head).addItems(items.tail) should be(UnpricedCart(items))
      }
    }

    "become unpriced" when {
      "a new item is added" in {
        PricedCart(items.head, USD(10.00)).addItem(items.last) should be(UnpricedCart(List(items.head, items.last)))
      }

      "new items are added" in {
        PricedCart(items.head, USD(10.00)).addItems(items.tail) should be(UnpricedCart(items))
      }
    }

    "remain priced" when {
      "a cost is set" in {
        PricedCart(items, USD(10.00)).withCost(JPY(1200)) should be(PricedCart(items, JPY(1200)))
      }
    }

    "become priced" when {
      "a cost is set" in {
        UnpricedCart(items).withCost(JPY(1200)) should be(PricedCart(items, JPY(1200)))
      }
    }

  }

}
