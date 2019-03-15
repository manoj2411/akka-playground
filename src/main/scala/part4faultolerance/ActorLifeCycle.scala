package part4faultolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifeCycle extends App {
  case object StartChild

  class LifeCycleActor extends Actor with ActorLogging {
    // life cycle methods
    override def preStart(): Unit = log.info("I am starting")
    override def postStop(): Unit = log.info("I have stopping")

    def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifeCycleActor], "child")
    }
  }

  val actorSystem = ActorSystem("LifeCycleDemo")
  val parent = actorSystem.actorOf(Props[LifeCycleActor], "parent")
  parent ! StartChild
  parent ! PoisonPill
}
