package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {

  // Part 1 - Actor systems
  val actorSystem = ActorSystem("firstSystem")
  println(actorSystem.name)

  /*
  - Its a heavy weight data structure that controls the no. of threads which then allocates to running actors
  - Its recommended to have 1 actorSystem for a application instance unless reasons to create more
  - Its name can only have alphanumeric chars, no spaces
    - Actors can be located by their actorSystems
  */

  // Part 2 - Create actors
  /*
  - Actors are like humans talking to each other
    - you send a msg and the other actor receives the msg when can hear it, they think and process it and they may or may not reply back
  - Actors are uniquely identified
  - Messages are async
  - Each actor may respond differently
  - Actors are really encapsulated, you cannot read the other actors mind or force them.
  */

  // Word count actor - create a class which extends Actor trait and define a method receive.
  class WordCountActor extends Actor {
    var totalWords = 0

    def receive: PartialFunction[Any, Unit] = {
      case message: String => {
        println("Received new message!")
        totalWords += message.split(" ").length
      }
      case message => println(s"[word counter] Unknown ${message.toString}")
    }
  }
  /*
    How to communicate - instantiate an actor then send it a message
      - The diff b/w normal object & actors is you can't instantiate an actor by calling new
    Part 3 - instantiate our actor
  */

  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  // This returns ActorRef, its the data structure that Akka gives us as a programmer so that we cannot call or poke into the actual wordCount actor instance. We can only communicate with this actor via actor reference.
  // Part 4 - cummunicate!
  wordCounter ! "I am learning akka"

  /*
    Only way to communicate to actors is by sending messages via ! (tell)
    ? How can we instantiate actor class with constructor arguments - by Prop with arguments
  */

  class Person(name: String) extends Actor {
    def receive: Receive = {
      case "hi" => println(s"$name received hi")
      case _ =>
    }
  }
  val person = actorSystem.actorOf(Props(new Person("Tom")))
  // This is not a good practice. Instead create a companion object and a method props which returns the object

  person ! "hi"
  person ! "Hey"
  

}
