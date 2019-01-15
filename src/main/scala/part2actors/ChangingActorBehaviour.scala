package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehaviour.Mom.Starter

object ChangingActorBehaviour extends App {
  /*
    Problem: We like to change our Actor behaviour with time, currently the only way is to keeping track the current state and do if/else in handler with that state.
    Scenario: We have 2 actors, Mother & Kid. Mother feeds her kid and kid changes his state depending on the food he gets. If he gets veggies the becomes sad, if he gets chocoate then he becomes Happy.
  */

  object FussyKid {
    val HAPPY = "happy"
    val SAD = "sad"
    case object Accept
    case object Reject
  }
  class FussyKid extends Actor {
    import FussyKid._
    import Mom._

    var state = HAPPY
    def receive: Receive = {
      case Food(VEGETABLES) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == SAD) sender ! Reject
        else sender ! Accept
    }
  }
  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive)
      case Food(CHOCOLATE) => // do nothing, already happy
      case Ask(_) => sender ! Accept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLES) => // do nothing, already sad
      case Food(CHOCOLATE) => context.become(happyReceive)
      case Ask(_) => sender ! Reject
    }
  }

  object Mom {
    val VEGETABLES = "veggies"
    val CHOCOLATE = "choco"
    case class Food(food: String)
    case class Ask(msg: String)
    case class Starter(kid: ActorRef)
  }
  class Mom extends Actor {
    import Mom._
    import FussyKid._

    def receive: Receive = {
      case Starter(kid) =>
        kid ! Food(VEGETABLES)
        kid ! Ask("Let's play")
        kid ! Food(CHOCOLATE)
        kid ! Ask("Let's play")
      case Accept => println("yay! happy kid")
      case Reject => println("sad, but healthy kid")
    }
  }

  val actorSystem = ActorSystem("changingBehaviour")
  val mom = actorSystem.actorOf(Props[Mom], "mom")
  val fussyKid = actorSystem.actorOf(Props[FussyKid], "fussyKid")
  mom ! Starter(fussyKid)
  //- logic for handling messages based on Actor state is bad, it can lead to very complex code since state can be complex.
  //- In prev examples we are var, which is bad. Let's create stateless fussyKid
  val statelessFussyKid = actorSystem.actorOf(Props[StatelessFussyKid], "statelessFussyKid")
  mom ! Starter(statelessFussyKid )

  Thread.sleep(1500)
}
