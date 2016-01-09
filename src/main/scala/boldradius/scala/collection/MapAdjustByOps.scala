package boldradius.scala.collection

class MapAdjustByOps[K, V](index: Map[K, V]) {

  /**
   * Apply a computation to the value at the given `key` and return an updated [[scala.collection.Map]].
   *
   * @param key Look up for value to be adjusted.
   * @param f Transform function.
   * @param filter Applied immediately to the result of `f`, and—if true—the new value is included, otherwise the original value is removed without a replacement.
   * @param alternative A default value to use if nothing matches `key`.
   */
  def adjustBy(key: K, f: V => V, filter: V => Boolean = _ => true, alternative: Option[V] = None): Map[K, V] = {
    val base: V =
      index
        .get(key)
        .orElse(alternative)
        .getOrElse(throw new IllegalArgumentException( s"""No value at key "$key" (no alternative provided)."""))

    val adjusted = f(base)

    if (filter(adjusted)) index + (key -> adjusted) else index - key
  }

}
