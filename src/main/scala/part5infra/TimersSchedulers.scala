package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import scala.concurrent.duration._

object TimersSchedulers extends App {

  /*
  Goal: run this code one second from now and at a defined point in the future maybe repeatedly.
    - Ex: run this code starting one second from now every three seconds.
    - schedulers and timers invented specifically for this.
  */
  class SimpleActor extends Actor with ActorLogging {
    def receive: Receive = {
      case msg => log.info(s"~~> $msg")
    }
  }

  val system = ActorSystem("TimersSchedulersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  system.log.info("Scheduling reminder!")
  import system.dispatcher

  system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "gentle reminder!"
  } // Scheduling of code has to happen on some threat much like futures. So we need an execution context.


  // Scheduling a repeating message
  val repeating = system.scheduler.schedule(1 second, 2 seconds) {
    simpleActor ! "heartbeat"
  }

  Thread.sleep(5000)
  repeating.cancel()

  //Thread.sleep(2000)
  system.terminate
}
