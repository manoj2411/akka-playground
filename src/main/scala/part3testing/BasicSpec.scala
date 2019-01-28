package part3testing

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class BasicSpec extends TestKit(ActorSystem("basicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {
  // hook used to turn down the test suit
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  // general structure
  "Thing being tested" should {
    "do this" in {
      // testing scenario
    }

    "do another thing" in {
      // another testing scenario
    }
  }

}

// Its recommended to create a companion object, here you store all the methods or all the values that you're going to use in your tests
object BasicSpec {

}