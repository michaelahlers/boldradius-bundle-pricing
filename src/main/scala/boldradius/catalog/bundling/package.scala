package boldradius.catalog

import java.util.concurrent.atomic.AtomicLong

package object bundling {

  case class RuleId(value: Long)

  object RuleId {

    private val id = new AtomicLong

    def next: RuleId = RuleId(id.getAndIncrement)

  }


}
