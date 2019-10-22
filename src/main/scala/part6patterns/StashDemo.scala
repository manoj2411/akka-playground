package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {


  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  class ResourceActor extends Actor with ActorLogging with Stash {
    var myData: String = ""

    def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening the state")
        unstashAll()
        context.become(open)
      case anyMsg =>
        log.info(s"Stashing $anyMsg bcz I am in closed state!")
        stash()
    }

    def open: Receive = {
      case Read =>
        log.info(s"[reading]: $myData")
      case Write(data) =>
        log.info(s"[writing]: $data")
        myData = data
      case Close =>
        log.info("closing the state")
        unstashAll()
        context.become(closed)
      case Open =>
        log.info("Stashing Open since I am already in Open state")
        stash()
    }
  }

  val system = ActorSystem("stashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor], "resource01")

//  resourceActor ! Write("Hey there!")
//  resourceActor ! Read
//  resourceActor ! Open

  resourceActor ! Read // Stash: [Read], mailbox: []
  resourceActor ! Open // Stash: [], mailbox: [Read] then process Read
  resourceActor ! Open // Stash: [Open]
  resourceActor ! Write("Hello!") // Process Write
  resourceActor ! Close // changing state to closed with unstash all Open messages if any
  resourceActor ! Read // since an Open was already in stash, after closed the state will open again & this will be processed.


  Thread.sleep(1000)
  system.terminate()

}
