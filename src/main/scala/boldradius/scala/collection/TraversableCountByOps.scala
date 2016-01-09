package boldradius.scala.collection

import scala.language.higherKinds

class TraversableCountByOps[A, C[A] <: Traversable[A]](c: C[A]) {

  def countBy[K](f: A => K): Map[K, Int] =
    c.groupBy(f).mapValues(_.size)

  def counted: Map[A, Int] = countBy(identity)

}
