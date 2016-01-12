package boldradius

import java.util.concurrent.atomic.AtomicLong

package object catalog {

  case class ItemId(value: Long)

  object ItemId {

    private val id = new AtomicLong

    def next: ItemId = ItemId(id.getAndIncrement)

  }

  case class RuleId(value: Long)

  object RuleId {

    private val id = new AtomicLong

    def next: RuleId = RuleId(id.getAndIncrement)

  }

}
