package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

// ### 2.2
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
    import Counter._

    var count = 0
    def receive: Receive = {
      case Incr => count += 1
      case Decr => count -= 1
      case Print => println("[counter]: " + count)
    }
  }
  import Counter._
  val counter = actorSystem.actorOf(Props[Counter], "counter01")
  (1 to 5).foreach(_ => counter ! Incr)
  counter ! Decr
  counter ! Print


  // Bank account
  object BankAccount {
    case class Deposit(amount: Double)
    case class Withdraw(amount: Double)
    case object Statement
    case class Success(msg: String)
    case class Failure(msg: String)
  }
  class BankAccount extends Actor {
    import BankAccount._
    var balance: Double = 0

    def receive: Receive = {
      case Deposit(amount) =>
        if (amount > 0) {
          balance += amount
          sender ! Success(s"Successfully add $amount")
        } else sender ! Failure("Invalid fund")
      case Withdraw(amount) =>
        if (amount <= balance && amount > 0) {
          balance -= amount
          sender ! Success(s"Successfully withdrew $amount")
        } else context.sender() ! Failure("Invalid fund")
      case Statement => sender ! "[bankAccount balance]: " + balance
    }
  }

  object AccountHolder {
    case class Starter(bankAccount: ActorRef)
  }

  class AccountHolder extends Actor {
    import BankAccount._
    import AccountHolder._

    def receive: Receive = {
      case Starter(bankAccount: ActorRef) =>
        bankAccount ! Deposit(-100)
        bankAccount ! Withdraw(200)
        bankAccount ! Statement
        bankAccount ! Deposit(500)
        bankAccount ! Withdraw(600)
        bankAccount ! Withdraw(100)
        bankAccount ! Statement
      case anyMessage => println(anyMessage)
    }
  }
  import AccountHolder._

  val account = actorSystem.actorOf(Props[BankAccount], "bankAccount01")
  val accountHolder = actorSystem.actorOf(Props[AccountHolder], "accHolder01")
  accountHolder ! Starter(account)

  /* Questions:
    1. How can we guarantee that the Print message is coming last since everything is async
        (1 to 5).foreach(_ => counter ! Incr)
        counter ! Decr
        counter ! Print
    2. Why we are adding side-effect to our Actor classes?
    3. Where is synchronisation primitives since it'll run in multi-threaded env?
    * Since akka messages are async/non-blocking, can we assume any ordering of messages?
    * If I send 2 messages to an actor are they competing for Actor's internal state(race condition)?
    * What does asynchronous means?
    * How does this all works since akka runs on JVM which knows only about Threads?

    Flow:
    - Actor has a receive Handler and some messages, queued into its mailbox.
    - So if you want to send a message to this actor, this is as simple as including another message into the actor's mailbox.
      - This is thread safe and it's being done by Akka behind the scenes
    - Now processing a message is more interesting since an actor is just a data structure. It needs a thread to actually execute any code
    - So at some point a thread will take control of this actor and it will start dequeing messages from the actor's mailbox and for every one of those messages it will invoke the message handler.
    - And as a result the actor might change his state or send messages to other actors.
    - And after that the message is simply discarded.
    - And this whole process happens a bunch of times.

    This whole process offers us some guarantees,
      1. Only 1 thread will operate on a actor at one time, which makes actors single threaded
        - that means we don't need to do any synchronization & locking, since only 1 thread has access to actor's internal state at any point of time
        - Thread will never leave the actor in the middle of processing message, makes message processing atomic
      2. Message delivery guarantees.
        - At most once delivery guarantee, receiving actor receives at most once, msgs never enqueued duplicates
        - For any sender-receiver pair, the message order is kept
  */
  Thread.sleep(1500)
  actorSystem.terminate
}
