package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorCapabilities.BankAccount.{Failure, Statement, Success}
import part2actors.ActorCapabilities.Counter.{Decr, Incr, Print}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    def receive: Receive = {
      case "Hello" => context.sender() ! "Hey, there! " // replying to a message
      case message: String => println(s"[simple actor $self] received: $message")
      case number: Int => println(s"[simple actor] received int: $number")
      case SpecialMessage(content) => println(s"[simple actor] received SpecialMessage: $content")
      case SayHelloTo(actorRef) => actorRef ! "Hello"
      case WirelessPhoneMessage(message, ref) =>
        println(s"[simple actor $self] received WirelessPhoneMessage: " + message)
        ref forward message + ":-)"
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
  /*
    Whenever an actor sends a message to another actor they pass themselves as a sender.
    So inside actor class:
      case SayHelloTo(actorRef) => actorRef ! "Hello" === (actorRef ! "Hello")(self)
    Q. Who is the sender in main App context?
      A. default sender - noSender = null
    Q. What if I reply to this noSender
      A. Dead letter encounters. Its a fake actor in akka which takes care of receive the messages that are not send to anyone.
  */


  // 4. Dead letters - when no actor to receive
  // 5. How actors forward messages
  //  - sending a message with the ORIGINAL sender.
  case class WirelessPhoneMessage(message: String, actorRef: ActorRef)
  tom ! WirelessPhoneMessage("Hello", jerry)

  /*
    Exercise:
    1. a Counter actor (has internal state of counter)
      - Increment
      - Decrement
      - Print
    2. a Bank account as an actor
      - Deposit amount
      - Withdraw amount
      - Statement
      - Deposit and Withdraw will reply Success/Failure instead of printing
      - interact with some other kind of actor
  */
  object Counter {
    case object Incr
    case object Decr
    case object Print

  }
  class Counter extends Actor {
    var count = 0
    def receive: Receive = {
      case Incr => count += 1
      case Decr => count -= 1
      case Print => println("[counter]: " + count)
    }
  }
  val counter = actorSystem.actorOf(Props[Counter], "counter")
  (1 to 5).foreach(_ => counter ! Incr)
  counter ! Decr
  counter ! Print


  // Bank account
  case class Deposit(amount: Double)
  case class Withdraw(amount: Double)
  object BankAccount {
    case object Statement
    case object Success
    case object Failure
  }
  class BankAccount extends Actor {
    var balance: Double = 0
    def receive: Receive = {
      case Deposit(amount) =>
        if (amount > 0) {
          balance += amount
          context.sender() ! Success
        } else context.sender() ! Failure
      case Withdraw(amount) =>
        if (amount <= balance) {
          balance -= amount
          context.sender() ! Success
        } else context.sender() ! Failure
      case Statement => println("[bankAccount balance]: " + balance)
    }
  }

  val account = actorSystem.actorOf(Props[BankAccount], "bankAccount")
  account ! Deposit(500)
  account ! Withdraw(600)
  account ! Withdraw(60)
  account ! Statement


}
