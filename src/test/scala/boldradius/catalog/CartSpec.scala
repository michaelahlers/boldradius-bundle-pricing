package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json._
import play.api.libs.json.{JsError, JsSuccess}
import squants.market.{JPY, USD}

class CartSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  val items =
    Item(SKU = "APPLE") ::
      Item(SKU = "BREAD") ::
      Item(SKU = "MARGARINE") ::
      Nil

  "Shopping carts" must {

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
        PricedCart(USD(10.00), items.head).addItem(items.last) should be(UnpricedCart(List(items.head, items.last)))
      }

      "new items are added" in {
        PricedCart(USD(10.00), items.head).addItems(items.tail) should be(UnpricedCart(items))
      }
    }

    "remain priced" when {
      "a cost is set" in {
        PricedCart(USD(10.00), items).withCost(JPY(1200)) should be(PricedCart(JPY(1200), items))
      }
    }

    "become priced" when {
      "a cost is set" in {
        UnpricedCart(items).withCost(JPY(1200)) should be(PricedCart(JPY(1200), items))
      }
    }

  }

  "JSON serializers" should {

    "read and write without loss" in {

      val exemplars: List[Cart] =
        UnpricedCart(items = Nil) ::
          UnpricedCart(items = items) ::
          PricedCart(items = Nil, cost = USD(10)) ::
          PricedCart(items = items, cost = USD(10)) ::
          Nil

      exemplars foreach { expected =>
        val actual = toJson(expected).validate[Cart]
        actual should be(JsSuccess(expected))
      }

    }

    "reject untyped documents" in {
      obj("items" -> items).validate[Cart] should be(a[JsError])
    }

    "reject corrupted documents" in {
      obj("_type" -> "bogus", "items" -> items).validate[Cart] should be(a[JsError])
    }

  }

}
