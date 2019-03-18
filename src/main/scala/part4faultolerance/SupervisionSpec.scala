package part4faultolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, Terminated}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll():Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisionSpec._

  "A supervisor" should {

    "resume its child in case of rum time exception" in {
      val superVisor = system.actorOf(Props[SuperVisor])
      superVisor ! Props[FussyWordCounter]

      val childRef = expectMsgType[ActorRef]
      childRef ! "I am learning scala!"
      childRef ! Report
      expectMsg(4)

      childRef ! "I am learning scala! with akka, it a different way of programming!"
      childRef ! Report
      expectMsg(4) // RuntimeException exception will be thrown and parent has strategy that
                        // Resume the child actor, means retain the original state.
    }

    "restart its child in case of empty string / NullPointerException" in {
      val superVisor = system.actorOf(Props[SuperVisor])
      superVisor ! Props[FussyWordCounter]

      val childRef = expectMsgType[ActorRef]
      childRef ! "I am learning scala!"
      childRef ! Report
      expectMsg(4)

      childRef ! ""
      childRef ! Report
      // I send the empty string it will throw the null pointer exception. The supervisor will
      // trigger its supervisor strategy with restart which clears the internal state so when
      // I send the Report message that number 0 will come up not 4.
      expectMsg(0)
    }

    "terminate its child in case of major error / IllegalArgumentException" in {
      val superVisor = system.actorOf(Props[SuperVisor])
      superVisor ! Props[FussyWordCounter]
      val childRef = expectMsgType[ActorRef]

      watch(childRef)
      childRef ! "random message"
      // I'm expecting this child to throw the IllegalArgumentException which gets signaled to its
      // supervisor parent and it will trigger the Directive associated (Stop) which is to Stop.
      // The child which is stopping the FussyWordCount which means that if the word counter has
      // stopped then I should receive a Terminated message because I've registered for Deathwatch.
      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == childRef)
    }

    "escalate an actor when it doesn't know what to do (throws Exception)" in {
      val superVisor = system.actorOf(Props[SuperVisor], "supervisor")
      superVisor ! Props[FussyWordCounter]
      val childRef = expectMsgType[ActorRef]

      watch(childRef)
      childRef ! 24
      // I should again expect this child to be Terminated. The reason for that is that when
      // the child receives 24 this case is handled which throws an exception. This gets signaled
      // to the supervisor which in turn goes to escalate. Now when an actor escalates it stops
      // all its children and escalates the failure to the parent.
      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == childRef)
    }
  }

  "A kind supervisor" should {
    "not kill children in failures, do restart" in {
      val superVisor = system.actorOf(Props[NoDeathOnRestartSupervisor])
      superVisor ! Props[FussyWordCounter]
      val childRef = expectMsgType[ActorRef]

      childRef ! "I am learning scala!"
      childRef ! Report
      expectMsg(4)

      childRef ! 24
      childRef ! Report

      expectMsg(0)

    }
  }
}



object SupervisionSpec {

  class SuperVisor extends Actor {
    // This is a member that's present in the actor trait
    override val supervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends SuperVisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty
    }
  }

  object Report
  class FussyWordCounter extends Actor {
    var words = 0
    def receive: Receive = {
      case "" => throw new NullPointerException("empty string")
      case Report => sender ! words
      case text: String =>
        if (text.length > 30)
          throw new RuntimeException("too large text")
        else if (Character.isLowerCase(text(0)))
          throw new IllegalArgumentException("first character must be uppercase ")
        else
          words += text.split(" ").length
      case _ => throw new Exception("accepts only string")

    }
  }
}
