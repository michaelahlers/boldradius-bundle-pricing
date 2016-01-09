package boldradius.catalog.bundling

import boldradius.squants.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import squants.Money

/**
 * Represents a single rule of a bundling.
 *
 * One apple “A” for \$1.99, or two apples “A” for \$2.15.
 *
 * {{{
Rule(SKUs = List("A", cost = USD(1.99))
Rule(SKUs = List("A", "A"), cost = USD(2.15))
 * }}}
 *
 * A loaf of bread “B” for \$5.00, a stick of margarine “M” for \$2.00, or loaf of bread “A” with two sticks of margarine “M” all together for \$7.00.
 *
 * {{{
Rule(SKUs = List("B", cost = USD(5.00))
Rule(SKUs = List("M"), cost = USD(2.00))
Rule(SKUs = List("B", "M", "M"), cost = USD(7.00))
 * }}}
 *
 * @param SKUs See [[boldradius.catalog.Item.SKU]].
 */
case class Rule(
  id: RuleId = RuleId.next,
  SKUs: List[String] = Nil,
  cost: Money
)

object Rule {

  implicit def reads: Reads[Rule] = (
    (__ \ 'id).read[Long].map(RuleId(_)) and
      (__ \ 'SKUs).read[List[String]] and
      (__ \ 'cost).read[Money]
    ) (Rule.apply _)

  implicit def writes: OWrites[Rule] = (
    (__ \ 'id).write[Long].contramap[RuleId](_.value) and
      (__ \ 'SKUs).write[List[String]] and
      (__ \ 'cost).write[Money]
    ) (unlift(Rule.unapply))

}
