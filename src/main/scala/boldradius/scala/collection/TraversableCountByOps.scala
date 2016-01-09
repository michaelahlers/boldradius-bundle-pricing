package boldradius.scala.collection

import scala.language.higherKinds

/**
 * Additional operations wrapper.
 *
 * {{{
 * import boldradius.scala.collection.withCountBy
 *
 * val chars = List('a', 'b', 'c', 'A', 'B', 'C', 'a', 'b', 'c')
 * val counts = chars.counted
 *
 * assert(chars.counted == Map('a' -> 2, 'b' -> 2, 'c' -> 2, 'A' -> 1, 'B' -> 1, 'C' -> 1))
 * assert(chars.countBy(_.toLower) == Map('a' -> 3, 'b' -> 3, 'c' -> 3))
 * }}}
 *
 * @see [[withCountBy]]
 */
class TraversableCountByOps[A, C[A] <: Traversable[A]](c: C[A]) {

  /**
   * A short-hand notation of [[scala.collection.Traversable.groupBy grouping]] all elements by a given identity function, and mapping over the values with [[scala.collection.Traversable.size size]].
   *
   * @param f Chooses which property of `A` to consider when grouping, which may be an instance of `A` itself.
   *
   * @tparam K Resulting key type from `f`.
   *
   * @return An index of occurrences by value identity.
   */
  def countBy[K](f: A => K): Map[K, Int] =
    c.groupBy(f).mapValues(_.size)

  /**
   * Variation on [[countBy]] which passes the [[scala.Predef.identity identity]] function by default.
   */
  def counted: Map[A, Int] = countBy(identity)

}
