package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class TimedAssertionsSpec extends TestKit(ActorSystem("TimedAssertionsSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll() = TestKit.shutdownActorSystem(system)
  import TimedAssertionsSpec._

  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])
    "reply in timely manner" in {
      within(500 millis, 1 second) {
        workerActor ! "work"
        expectMsg(WorkResult(24))
      }
    }

    "reply with valid work at a reasonable interval" in {
      within(1 second) {
        workerActor ! "workSequence"

        val results: Seq[Int] = receiveWhile[Int](max = 2.seconds, idle = 500 millis, messages = 10) {
          case WorkResult(res) => res
        }

        assert(results.sum > 5)
      }
    }
  }
}

object TimedAssertionsSpec {
  case class WorkResult(result: Int)

  class WorkerActor extends Actor {
    def receive: Receive = {
      case "work" =>
        // long computation
        Thread.sleep(500)
        sender ! WorkResult(24)
      case "workSequence" =>
        val rand = new Random
        for (_ <- 1 to 10) {
          Thread.sleep(rand.nextInt(50))
          sender ! WorkResult(1)
        }
    }
  }
}
