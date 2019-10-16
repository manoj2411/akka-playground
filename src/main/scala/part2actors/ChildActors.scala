package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

// ### 2.4
object ChildActors extends App {

  /* ## Child creation
    Long story short is that actors can create other actors using actor's *context.actorOf()*
    - This child creation feature gives akka the ability to create actor hierarchies
      parent -> child -> grandChild
    Question: child actor owned by parent but who owns parent?
      Ans: We have Guardian actors (top-level). Every Akka actors system has three Guardian
        1. /system - system guardian. Every actor system has its own actors for managing various things and for example managing the logging
        2. /user - user level guardian. Every actor that we (as programmer) create using System that actorOf are actually owned by this /user
        3. / - the root guardian. It manages both user and system guardian
    ## Actor Selection
     Akka gives feature to find actor by path, called **actor selection**.
      val childSelection = system.actorSelection("user/parent/child")
     - this will return ActorSelection, its a wrapper over a potential ActorRef, that can be used to send msg.
      childSelection ! "Hey, I found you"
     - If the path is incorrect then ActorSelection object will contain no actor and the msg will be dropped.

    ## Danger!
      NEVER PASS MUTABLE ACTOR STATE, OR THE `this` REFERENCE TO CHILD ACTORS.
      NEVER IN YOUR LIFE.
    - In practice the behavior in which an actor's state is suddenly becomes changed without the use of messages is extremely extremely hard to debug especially in a critical environment like a banking system.
    - Calling a method directly on an actor bypasses all the logic and all the potential security checks that it would normally do in its receive message handler.
    - The problem is calling a method directly on an actor instance, we never directly call methods on actors. We only send messages.
  */

  /* Exercise: Distributed word counting
     - Use akka parallelism and distribute the work tasks to a number of actors.
     - Crate a small actor hierarchy where a number of actors will be workers and there will be
        master actor that manages them. Its a task that will take some time and easily parallelizable.
  */

  object WordCountMaster {
    case class Initialize(vChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }

  class WordCountMaster extends Actor {
    import WordCountMaster._

    def receive: Receive = {
      case Initialize(nchildren) if nchildren <= 0 => println("Children be a positive number")
      case Initialize(nchildren) =>
        val children = for (i <- 1 to nchildren) yield
          context.actorOf(Props[WordCountWorker], s"childWorker_$i")
        context.become(withChildren(children, 0, 1, Map()))
    }

    def withChildren(children: Seq[ActorRef], currWorker: Int, currId: Int, senderMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        children(currWorker) ! WordCountTask(currId, text)
        val nextWorkerIndex = (currWorker + 1) % children.length
        val newMapping = senderMap + (currId -> context.sender)
        val nextId = currId + 1

        context.become(withChildren(children, nextWorkerIndex, nextId, newMapping))
      case WordCountReply(id, count) =>
        println(s"received result for id: ${id} -> ${count}")
        val sourceSender: ActorRef = senderMap(id)
        sourceSender ! count
        val newMapping = senderMap - id

        context.become(withChildren(children, currWorker, currId, newMapping))
    }

  }

  class WordCountWorker extends Actor {
    import WordCountMaster._

    def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"[${self.path}] processing: ${id} -> $text")
        sender ! WordCountReply(id, text.split(' ').length)
    }
  }
  /* Flow:
      - create WCMaster
      - send Initialize(10)
      - send "scala is awesome" to WCMaster
        - wcm will send WordCountTask("...") to 1 of its child
          - child replies with WordCountReply(3) to master
        - master replies with 3 to sender
      Use Round-Robin to distribute tasks
  */
  class Runner extends Actor {
    import WordCountMaster._

    def receive: Receive = {
      case "start" =>
        val wcm = context.actorOf(Props[WordCountMaster], "master")
        wcm ! Initialize(3)
        List("Scala is awesome", "Learning akka system is different", "End", "Hey there!", "")
            .foreach { wcm ! _ }

      case count : Int => println(s"[WC received]: $count")
    }
  }

  val actorSystem = ActorSystem("childActors")
  val runner = actorSystem.actorOf(Props[Runner])
  runner ! "start"

  Thread.sleep(2000)
  actorSystem.terminate
}

