package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing._
import com.typesafe.config.ConfigFactory

object AkkaRouters extends App {
  /*
    - Routers are extremely useful when you want to delegate or spread work in between
  multiple actors of the same kind
    - Routers are usually middle level actors that forward messages to other actors.
  */
  class Master extends Actor {
    // STEP 1: create routees
    private val slaves = for (i <- (1 to 5)) yield {
      val slaveRef = context.actorOf(Props[Slave], s"slave$i")
      context.watch(slaveRef)
      ActorRefRoutee(slaveRef)
    }

    /* Akka supported options for routing logic:
        * round-robin
        * random
        * smallest mailbox
        * broadcast
        * scatter-gather-first
        * tail-chopping
        * consistent hashing
    * */
    // STEP 2: create router
    private var router = Router(RoundRobinRoutingLogic(), slaves)

    def receive: Receive = {
      // STEP 4: handle termination of routees
      case Terminated(terminatingSlave) =>
        router = router.removeRoutee(terminatingSlave)
        val slaveRef = context.actorOf(Props[Slave], "slave*")
        context.watch(slaveRef)
        router = router.addRoutee(slaveRef)
      // STEP 3: route the message
      case message =>
        router.route(message, sender)
    }
  }

  class Slave extends Actor with ActorLogging {
    def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val actorSystem = ActorSystem("routersDemo", ConfigFactory.load().getConfig("routersDemo"))
//  val master = actorSystem.actorOf(Props[Master], "master")
//  for (i <- (1 to 10)) {
//    master ! s"message no $i"
//  }


  /* Pool Router: Simpler method for creating a router actor with its own child */
//  val poolMaster = actorSystem.actorOf(RoundRobinPool(5).props(Props[Slave]), "aPoolMaster")
//  for ( i <- (1 to 10)) {
//    poolMaster ! s"message no $i"
//  }
  /* Creating pool master router using config
      - name must be exact same as the 1 written in config
  */
  val poolMaster2 = actorSystem.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  for ( i <- (1 to 10)) {
    poolMaster2 ! s"message no $i"
  }

  Thread.sleep(1000)
  actorSystem.terminate()
}
