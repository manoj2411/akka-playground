package part3testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

/* A TestProb is a special type of actors with assertion capabilities  */
class TestProbsSpec extends TestKit(ActorSystem("TestProbsSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }
  import TestProbsSpec._

  "A master sctor" should {
    "register a slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave_01")

      master ! Register(slave.ref)
      expectMsg(RegisterAck)
    }

    "send work to the slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave_01")
      master ! Register(slave.ref)

      val workText = "I am testing akka"
      master ! Work(workText)
      expectMsg(RegisterAck)
      // *testActor* is the member of test kit which is implicitly passed as the sender of every single message
      slave.expectMsg(SlaveWork(workText, testActor))
      // testing interaction b/w master and slave actor
      // Nicest thing about testProbe is that they can be **instructed to send or reply **with messages.
      slave.reply(WorkCompleted(4, testActor)) // mocking the reply to the master back
      expectMsg(Report(4)) // testActor received the Report msg
    }
  }
}

object TestProbsSpec {
  /* Scenario: word count actors hierarchy, master-slave
     Communication:
      - send some work to master
        - master sends some work to slave
        - slave do processing and send result back to master
        - master aggregates the result
      - master send back the result to the original requester
  */
  case class Register(slave: ActorRef)
  case object RegisterAck
  case class Work(text: String)
  case class SlaveWork(text: String, sender: ActorRef)
  case class WorkCompleted(count: Int, originalSender: ActorRef)
  case class Report(count: Int)

  class Master extends Actor {
    def receive: Receive = {
      case Register(slaveRef) =>
        sender ! RegisterAck
        context.become(online(slaveRef, 0))
      case _ => //
    }

    def online(slaveRef: ActorRef, wordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender)
      case WorkCompleted(count, originalSender) =>
        val newWordCount = wordCount + count
        originalSender ! Report(newWordCount)
        context.become(online(slaveRef, newWordCount))
    }
  }
}
