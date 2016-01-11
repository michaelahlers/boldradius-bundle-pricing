package boldradius.catalog

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.BeforeAndAfterAll

class AkkaPricerSpec
  extends PricerSpec
          with BeforeAndAfterAll
          with LazyLogging {

  val system: ActorSystem = ActorSystem()

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  override def pricer: Pricer = new AkkaPricer(system)

}
