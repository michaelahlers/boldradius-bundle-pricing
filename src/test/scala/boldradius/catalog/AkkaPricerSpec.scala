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

  /* TODO: Restore once spurious matches are reduced. */
  //"Solve reasonably fast" when {
  //
  //  "using applying all rules against a large cart" in {
  //    val items = Inventory.all
  //    val rules = Rules.all
  //    pricer(rules, items).futureValue(Timeout(Span(60, Seconds)), Interval(Span(1, Seconds))) should be(USD(7.25))
  //  }
  //
  //}

}
