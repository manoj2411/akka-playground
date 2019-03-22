package part4faultolerance

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{Backoff, BackoffSupervisor}

import scala.io.Source
import scala.concurrent.duration._

object BackoffSupervisorPattern extends App {

  case object ReadFile
  class FilePersistentActor extends Actor with ActorLogging {
    var sourceFile: Source = null

    override def preStart(): Unit = log.info("Persistent actor starting")
    override def postStop(): Unit = log.info("Persistent actor stopped")
    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info("Persistent actor restarting")

    def receive: Receive = {
      case ReadFile =>
        if (sourceFile == null)
          sourceFile = Source.fromFile(new File("src/main/resources/testfiles/file.txt"))
        log.info("Reading data from file: " + sourceFile.getLines.toList)
    }
  }
  val system = ActorSystem("BackoffSupervisorPattern")
  // val actor = system.actorOf(Props[FilePersistentActor], "filePersistor")
  // actor ! ReadFile


  val supervisorProps = BackoffSupervisor.props {
    Backoff.onFailure(Props[FilePersistentActor], "backoffActor", 2.seconds, 30.seconds, 0.3)
  }

  /* Backoff supervisor pattern
      - solve a big pain that we often see in practice which is the repeated restarts of actors
      - Ex: a database if a database goes down and many actors are trying to read or write at
          that database, many actors will start to throw exceptions, will start supervisions strategies.
      - If the actors restart at the same time the database might go down again or the actors might
          get into a blocking state.
      - The backoff supervisor pattern introduces exponential delays and randomness in
          between the attempts to rerun a supervision strategy.
  */

  val backoffSupervisor = system.actorOf(supervisorProps, "supervisor")
  backoffSupervisor ! ReadFile

  Thread.sleep(10000)
  system.terminate
}
