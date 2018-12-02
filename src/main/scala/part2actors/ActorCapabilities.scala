package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    def receive: Receive = {
      case message: String => println(s"[simple actor] received: $message")
      case number: Int => println(s"[simple actor] received int: $number")
      case SpecialMessage(content) => println(s"[simple actor] received SpecialMessage: $content")
    }
  }

  val actorSystem = ActorSystem("actorCapabilitesSystem")
  val simpleActor = actorSystem.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "Hey actor!"
  //  Things to remember:
  // 1. Messages can be of any type. Not just primitive types, you can define your own.
  simpleActor ! 24

  case class SpecialMessage(msg: String)
  simpleActor ! SpecialMessage("this is special object!")
}
