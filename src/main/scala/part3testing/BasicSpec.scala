package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

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

  import BasicSpec._

  // how to do async assertions
  "A SimpleActor" should {
    "send back the same message" in {
      val simpleActor = system.actorOf(Props[SimpleActor])
      val msg = "Hey actor"
      simpleActor ! msg

      expectMsg(msg) // akka.test.single-expect-default
    }
  }

  "A Blackhole" should {
    "send back the same message" in {
      val sallowActor = system.actorOf(Props[Blackhole])
      val msg = "Hey actor"
      sallowActor ! msg

      expectNoMessage(1 second)
    }
  }

  "A lab test actor" should {
    val labActor = system.actorOf(Props[LabActor])

    "turn string into uppercase" in {
      labActor ! "Hey actor"
      val reply = expectMsgType[String]

      assert(reply == "HEY ACTOR")
    }

    "reply (randomly) to a greeting" in {
      labActor ! "Greeting"
      expectMsgAnyOf("Hi", "Hello")
    }

    "reply with fav tech" in {
      labActor ! "favTech"
      expectMsgAllOf("Scala", "Akka")
    }

    "reply with fav tech in different way" in {
      labActor ! "favTech"
      val messages = receiveN(2) // returns Seq[Any]
      // Do complicated assertions
      assert(messages.length == 2)
    }

    "reply with fav tech in fancy way" in {
      labActor ! "favTech"
      // This is very powerful.
      expectMsgPF() {
        case "Scala" => // only care that the PF is defined for the messages that we care about
        case "Akka" =>
      }
    }
  }
}

// Its recommended to create a companion object, here you store all the methods or all the values that you're going to use in your tests
object BasicSpec {
  class SimpleActor extends Actor {
    def receive: Receive = {
      case msg => sender ! msg
    }
  }

  class Blackhole extends Actor {
    def receive: Receive = Actor.emptyBehavior
  }

  class LabActor extends Actor {
    val random = new Random()
    def receive: Receive = {
      case "Greeting" => sender ! (if (random.nextBoolean()) "Hello" else "Hi")
      case "favTech" =>
        sender ! "Akka"
        sender ! "Scala"
      case msg: String => sender ! msg.toUpperCase
    }
  }
}