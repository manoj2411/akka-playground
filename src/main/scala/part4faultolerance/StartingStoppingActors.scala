package part4faultolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object StartingStoppingActors extends App  {

  val actorSystem = ActorSystem("startStopActorDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    import Parent._

    def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"starting child $name")
        val newChild = context.actorOf(Props[Child], name)
        context.become(withChildren(children + (name -> newChild)))
//      case StopChild(name) =>

    }
  }

  class Child extends Actor with ActorLogging {
    def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }

  import Parent._
  val parentActor = actorSystem.actorOf(Props[Parent], "parent")
  parentActor ! StartChild("firstChild")
  val child1 = actorSystem.actorSelection("/user/parent/firstChild")
  child1 ! "Hey kido!"

  Thread.sleep(1000)
  actorSystem.terminate
}
