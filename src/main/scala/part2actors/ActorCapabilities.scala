package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    def receive: Receive = {
      case "Hello" => context.sender() ! "Hey, there! " // replying to a message
      case message: String => println(s"[simple actor $self] received: $message")
      case number: Int => println(s"[simple actor] received int: $number")
      case SpecialMessage(content) => println(s"[simple actor] received SpecialMessage: $content")
      case SayHelloTo(actorRef) => actorRef ! "Hello"
      case WirelessPhoneMessage(message, ref) => ref forward message + ":-)"
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

  /*
    We can send any messages with 2 conditions
    a. messages must be IMMUTBABLE
    b. messages must be SERIALIZABLE - JVM should be able to convert it to byte code and send to another JVM
    Always remember these principles.
  */

  /*
    2. Actors have information about their context and about themselves.
       Each actor has a member called context, its a complex data-structure that has references to
       information regarding the environment this actor is running.
       Ex:
        - it has access to the actor system (`context.system`)
        - it has access to this actor's own actor reference (`context.self`)
          - we can use self ref to send a messages to ourselves.
          `context.self === this`
  */


  //3. How actors can REPLY to the messages

  val tom = actorSystem.actorOf(Props[SimpleActor], "tom")
  val jerry = actorSystem.actorOf(Props[SimpleActor], "jerry")
  case class SayHelloTo(ref: ActorRef)
  tom ! SayHelloTo(jerry)

  // 4. Dead letters - when no actor to receive
  // 5. How actors forward messages
  //  - sending a message with the ORIGINAL sender.
  case class WirelessPhoneMessage(message: String, actorRef: ActorRef)
  tom ! WirelessPhoneMessage("Hello", jerry)



}