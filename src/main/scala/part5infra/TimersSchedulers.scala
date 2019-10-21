package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._

// ### 5.1
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

//  system.log.info("Scheduling reminder!")
  import system.dispatcher

//  system.scheduler.scheduleOnce(1 second) {
//    simpleActor ! "gentle reminder!"
//  } // Scheduling of code has to happen on some threat much like futures. So we need an execution context.


  // Scheduling a repeating message
//  val repeating = system.scheduler.schedule(1 second, 2 seconds) {
//    simpleActor ! "heartbeat"
//  }

//  Thread.sleep(5000)
//  repeating.cancel()


  /* Exercise: Implement a self closing actor
  - If actor receives a message then you 1 second to send it another message
  - Otherwise actor will stop itself
  - If another messages comes then the time window resets
  */

  class SelfClosingActor extends Actor with ActorLogging {
    var lastScheduled = schedule

    def schedule: Cancellable =   context.system.scheduler.scheduleOnce( 1 second ) {
      self ! "stop"
    }

    def receive: Receive = {
      case "stop" =>
        context.stop(self)
      case message =>
        log.info(s"[message]: $message")
        lastScheduled.cancel()
        lastScheduled = schedule
    }
  }

  val selfClosingActor = system.actorOf(Props[SelfClosingActor])
//  selfClosingActor ! "first message"
//  system.scheduler.scheduleOnce(500 millis ) {
//    selfClosingActor ! "ping"
//  }

//  system.scheduler.scheduleOnce(2000 millis ) {
//    selfClosingActor ! "pong"
//  }


  // Timers: used to schedule message to self, from within

  val timerKey = "demoTimer"
  class TimerBasedActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(timerKey, "start", 500 millis)

    def receive: Receive = {
      case "start" =>
        log.info("Starting timer actor")
        timers.startPeriodicTimer(timerKey, "ping", 1 second) // reusing same timerKey since it was Single Timer
      case "ping" =>
        log.info("received ping!")
      case "stop" =>
        log.warning("Stopping timer actor!")
        timers.cancel(timerKey)
        context.stop(self)
    }
  }

  val timerActor = system.actorOf(Props[TimerBasedActor], "timerActor")
  system.scheduler.scheduleOnce(5 seconds) {
    timerActor ! "stop"
  }


  Thread.sleep(7000)
  system.terminate
}
