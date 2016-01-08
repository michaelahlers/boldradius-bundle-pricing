package boldradius

import java.util.concurrent.atomic.AtomicLong

package object catalog {

  case class ItemId(value: Long)

  object ItemId {

    private val id = new AtomicLong

    def next: ItemId = ItemId(id.getAndIncrement)

  }

}
