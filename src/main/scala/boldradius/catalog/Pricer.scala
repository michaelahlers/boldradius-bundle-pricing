package boldradius.catalog

import boldradius.catalog.bundling.Rule
import squants.market.Money

import scala.concurrent.Future

/**
 * A (typical) [[Pricer]] implementation searches for the lowest price for a shopping cart, given rules which may provide discounts under certain combinations of items.
 *
 * == Sample Problem ==
 *
 * The order in which rules are evaluated against a cart can affect the outcome (''e.g.'', by eliminating potential bulk discounts). Consider a scenario with rules `{ {A}, {B}, {A,A}, {A,B} }` for cart `{ a, a, b }`.
 *
 * These paths all the way those rules may be applied:
  1. `{ {A}, {A}, {B} }`
  1. `{ {A}, {A,B} }`
  1. `{ {B}, {A}, {A} }`
  1. `{ {B}, {A,A} }`
  1. `{ {A,A}, {B} }`
  1. `{ {A,B}, {A} }`
 *
 * Spurious paths removed:
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
 * Implementations here '''are not performant'''. They are good enough for homework. Optimizations applying rules in order of decreasing complexity are pending.
 *
 * A comprehensively more sophisticated approach involves using a [[http://en.wikipedia.org/wiki/Constraint_programming constraint solver]] (''e.g.'', [[http://choco-solver.org Choco]], [[http://bach.istc.kobe-u.ac.jp/copris Copris]]) or [[http://wikipedia.org/wiki/Simulated_annealing simulated annealing]].
 *
 * @see [[http://wikipedia.org/wiki/Knapsack_problem Knapsack problem]] ([[http://wikipedia.org Wikipedia]])
 */
trait Pricer {

  final def apply(rules: List[Rule], cart: Cart): Future[Money] =
    apply(rules = rules, items = cart.items)

  final def apply(rules: List[Rule], items: Item*): Future[Money] =
    apply(rules = rules, items = items.toList)

  /**
   * Given the provided [[bundling.Rule rules]], calculate the lowest price for the [[Item items]].
   */
  def apply(rules: List[Rule], items: List[Item]): Future[Money]

}

object Pricer {

  case class UnmatchedItemsException(rules: List[Rule], items: Set[Item]) extends Exception {
    override def getMessage: String =
      """Items in %s didn't match any rules in %s."""
        .format(
          items.map(_.SKU).mkString("{", ", ", "}"),
          rules.map(_.SKUs.mkString("{", ", ", "}")).mkString("{", ", ", "}")
        )
  }

}
