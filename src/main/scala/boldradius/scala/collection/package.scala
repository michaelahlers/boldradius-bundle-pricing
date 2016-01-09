package boldradius.scala

import scala.language.{higherKinds, implicitConversions}

package object collection {

  /**
   * Import to augment any [[scala.collection.Traversable]] with [[TraversableCountByOps.countBy]].
   */
  implicit def withCountBy[A, C[A] <: Traversable[A]](c: C[A]): TraversableCountByOps[A, C] = new TraversableCountByOps(c)

}
