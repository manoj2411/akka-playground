package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

// ### 2.5
object ActorLoggingDemo extends App {

  class SimpleAcotor extends Actor {
    val logger = Logging(context.system, this)

    def receive: Receive = {
      case message => logger.info("[explicit logging]: {}", message)
    }
  }
  val system = ActorSystem("LoggingDemo")
  val simpleAcotor = system.actorOf(Props[SimpleAcotor])
  simpleAcotor ! "Msg sample"

  class ActorWithLogging extends Actor with ActorLogging {
    def receive: Receive = {
      case message => log.info("[implicit logging]: {}", message)
    }
  }

  val anotherAcotor = system.actorOf(Props[ActorWithLogging])
  anotherAcotor ! "Another msg sample"


  Thread.sleep(1000)
  system.terminate
}
