package part4faultolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

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
      case StopChild(name) =>
        log.info(s"stopping actor $name")
        val childOption = children.get(name)
        // context.stop is a non blocking method. It just gives a signal to stop child which
        // will take some time and stop it sometime later.
        childOption.foreach { childRef => context.stop(childRef)}
      case Stop =>
        log.info("stopping itself")
        context.stop(self) // this also stops all its childActors
    }
  }

  class Child extends Actor with ActorLogging {
    def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }

  // 1. Stopped actors using context.stop
  import Parent._
//  val parentActor = actorSystem.actorOf(Props[Parent], "parent")
//  parentActor ! StartChild("firstChild")
//  parentActor ! StartChild("secondChild")
//  val child1 = actorSystem.actorSelection("/user/parent/firstChild")
//  child1 ! "Hey kido!"
//
//  parentActor ! StopChild("firstChild")
//  parentActor ! Stop
//
//  // 2. Stopping actors using special messages:
//  // PoisonPill & Kill
//  val child2 = actorSystem.actorOf(Props[Child])
//  child2 ! "You are getting Poisoned!"
//  child2 ! PoisonPill
//  child2 ! "Still alive?"
//
//  val child3 = actorSystem.actorOf(Props[Child])
//  child3 ! "killing you!"
//  child3 ! Kill
//  child2 ! "Still alive?"

  // 3. Death watch: get notified when an Actor dies
  class Watcher extends Actor with ActorLogging {
    import Parent._

    def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"watching actor $name")
        // register THIS actor for the death of Child, will receive Terminated msg
        context.watch(child)
        //context.unwatch(): used to unwatch the actor when you are no more interested.
      case Terminated(actorRef) =>
        log.info(s"Watched actor is terminated: $actorRef")
    }
  }

  val watcher = actorSystem.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watchedChild")
  Thread.sleep(200)
  val watchedChild = actorSystem.actorSelection("/user/watcher/watchedChild")
  watchedChild ! PoisonPill


  Thread.sleep(1000)
  actorSystem.terminate

}
