package part4faultolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

// ### 4.2
object ActorLifeCycle extends App {
  case object StartChild

  class LifeCycleActor extends Actor with ActorLogging {
    // life cycle methods
    override def preStart(): Unit = log.info("I am starting")
    override def postStop(): Unit = log.info("I have stopped")

    def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifeCycleActor], "child")
    }
  }

  val actorSystem = ActorSystem("LifeCycleDemo")
//  val parent = actorSystem.actorOf(Props[LifeCycleActor], "parent")
//  parent ! StartChild
//  parent ! PoisonPill

  /*
  * Restarting hooks
  */
  case object Fail
  case object FailChild
  class Parent extends Actor {
    private var child = context.actorOf(Props[Child], "child")

    def receive: Receive = {
      case FailChild => child ! Fail
    }
  }

  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info(s"I am going to start - ${self} - ${this}")
    override def postStop(): Unit = log.info(s"I have stopped - ${self} - ${this}")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info(s"child restarting because of: ${reason.getMessage} - ${self} - ${this}")
    override def postRestart(reason: Throwable): Unit =
      log.info(s"child restarted - ${self} - ${this}")

    def receive: Receive = {
      case Fail =>
        log.warning("I am going to Fail")
        throw new RuntimeException("random failing!")
    }
  }

  val parent = actorSystem.actorOf(Props[Parent], "parent")
  parent ! FailChild

  /* Q. Why after throwing exception actor restarted and started working again?
     A. Its a part of default supervision strategy which says if an actor threw an exception
     while processing a message, that message will removed from the queue(mail box) and
     the actor is restarted.
  */
}
