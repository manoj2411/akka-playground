package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object AkkaConfigIntro extends App {
  // config is just a text, described in name = value pairs
  class SimpleActor extends Actor with ActorLogging {
    def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }
  // 1. Inline config
  val confString =
    """
      |akka {
      | loglevel = "Error"
      |}
    """.stripMargin
  val conf = ConfigFactory.parseString(confString)
  val system = ActorSystem("loggingDemo", ConfigFactory.load(conf))
  val actor = system.actorOf(Props[SimpleActor])
  actor ! "Hey, I am here."

  // 2. conf file. Akka looks and loads default config from resources/application.conf
  val defaultSystem = ActorSystem("loggingDemoDefault")
  val defaultActor = defaultSystem .actorOf(Props[SimpleActor])
  defaultActor ! "Hey, I am from default."

  // 3. Load config from different file
  val confFromDifferentFile = ConfigFactory.load("myConf")
  println("confFromDifferentFile: " + confFromDifferentFile.getInt("foo.bar"))

  // 4. Load config from different format i.e. JSON
  val confFromJsonFile = ConfigFactory.load("json/anotherConf.json")
  println("confFromJsonFile: " + confFromJsonFile .getString("appName"))
  println("concurrency " + confFromJsonFile .getInt("akka.concurrency"))


  Thread.sleep(1000)
  system.terminate
  defaultSystem.terminate
}
