package boldradius.catalog

import boldradius.catalog
import squants.market.Money

import scala.concurrent.{ExecutionContext, Future}

/**
 * A (typical) [[Pricer]] implementation searches for the lowest price for a shopping cart, given rules which may provide discounts under certain combinations of items.
 *
 * == Usage Notes ==
 *
 * Simply define [[Item items]], [[Rule rules]], and pick a [[Pricer pricer]]. This API is unopinionated about how rules are defined, but it deliberately avoids fractional price arithmetic.
 *
 * See the following example:
 *
 * {{{
import boldradius.catalog._
import squants.market.USD

import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/* Define inventory items. */

object Bread extends Item(SKU = "BREAD")

object Margarine extends Item(SKU = "MARGARINE")

/* Define how they are priced. */

val B = Rule(USD(3.00), Bread)
val M = Rule(USD(2.50), Margarine)

/** Two for the price of one discount. */
val BMM = Rule(B.cost + M.cost, Bread, Margarine, Margarine)

val rules = List(B, M, BMM)

val pricer: Pricer = new ChocoPricer

/** Customer buys one loaf of bread. */
assert(result(pricer.apply(rules, Cart(Bread)), 1 second).cost == USD(3.00))

/** Or buys one stick of margarine. */
assert(result(pricer.apply(rules, Cart(Margarine)), 1 second).cost == USD(2.50))

/** Or buys bread and margarine together. */
assert(result(pricer.apply(rules, Cart(Bread, Margarine)), 1 second).cost == USD(5.50))

/** But may receive a free stick. */
assert(result(pricer.apply(rules, Cart(Bread, Margarine, Margarine)), 1 second).cost == USD(5.50))
 * }}}
 *
 * More complete examples may be found in the respective unit test.
 *
 * == Sample Problem ==
 *
 * The order in which rules are evaluated against a cart can affect the outcome (''e.g.'', by eliminating potential bulk discounts). Consider a scenario with rules `{ {A}, {B}, {A,A}, {A,B} }` for cart `{ a, a, b }`.
 *
 * Those rules may be applied as:
  1. `{ {A}, {A}, {B} }`
  1. `{ {A}, {A,B} }`
  1. `{ {B}, {A}, {A} }`
  1. `{ {B}, {A,A} }`
  1. `{ {A,A}, {B} }`
  1. `{ {A,B}, {A} }`
 *
 * With spurious paths removed:
  1. `{ {A}, {A}, {B} }`
  1. `{ {A}, {A,B} }`
  1. `{ {B}, {A,A} }`
 *
 * Finally, where `{A}` is \$3.00, `{B}` is \$2.00, `{A,A}` is \$5.00, `{A,B}` is \$3.00, total costs for each respective combination are:
  1. \$3.00 + \$3.00 + \$2.00 = \$8.00
  1. \$3.00 + \$3.00 = \$6.00
  1. \$2.00 + \$5.00 = \$7.00
 *
 * == Performance Notes ==
 *
 * Implementations here '''are not performant'''. They are good enough for homework. A useful exercise entails employing a [[http://en.wikipedia.org/wiki/Constraint_programming constraint solver]] (''e.g.'', [[http://choco-solver.org Choco]], [[http://bach.istc.kobe-u.ac.jp/copris Copris]]).
 *
 * @see [[http://wikipedia.org/wiki/Knapsack_problem Knapsack problem]] ([[http://wikipedia.org Wikipedia]])
 */
trait Pricer {

  final def apply(rules: List[Rule], cart: Cart)(implicit ec: ExecutionContext): Future[PricedCart] =
    apply(rules = rules, items = cart.items).map(cart.withCost)

  final def apply(rules: List[Rule], items: Item*): Future[Money] =
    apply(rules = rules, items = items.toList)

  /**
   * Given the provided [[catalog.Rule rules]], calculate the lowest price for the [[Item items]].
   */
  def apply(rules: List[Rule], items: List[Item]): Future[Money]

}

object Pricer {

  case class UnmatchedItemsException(/*rules: List[Rule], */ items: Set[Item]) extends Exception {
    override def getMessage: String =
      """Items in %s didn't match any rules.""" // in %s."""
        .format(
        items.map(_.SKU).mkString("{", ", ", "}")
        // ,rules.map(_.SKUs.mkString("{", ", ", "}")).mkString("{", ", ", "}")
      )
  }

}
