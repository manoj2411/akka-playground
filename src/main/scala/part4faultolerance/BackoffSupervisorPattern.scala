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

  // val backoffSupervisor = system.actorOf(supervisorProps, "supervisor")
  // backoffSupervisor ! ReadFile

  class EagerFilerPersistentActor extends FilePersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting")
      sourceFile = Source.fromFile(new File("src/main/resources/testfiles/invalid_file.txt"))
    }
  }
  //val eagerActor = system.actorOf(Props[EagerFilerPersistentActor], "eager01")
  // => It'll throw ActorInitializationException and the strategy is STOP


  // Instead of creating eagerActor directly lets create it with supervisor.
  // we want this Backoff to kick in when the actor is STOPPED.

  val newSupervisorProps = BackoffSupervisor.props(
    // this Backoff will kick in when the actor is STOPPED.
    Backoff.onStop(Props[EagerFilerPersistentActor], "eager02", 1 second, 24 second, 0.2)
  )

  val newSupervisor = system.actorOf(newSupervisorProps, "eagerSupervisor")
  // This will create "eagerSupervisor" and child "eager02" actor.
  // This child will die on initialization, supervision strategy will kick in and trigger Backoff.
  // It'll attempts the restart of "eager02" actor exponentially.

  /*
  - The simulation of what would happen in real life when you want to connect to an external data
  source like a database or network connection that might suddenly go down and cause your actress
  to fail with some kind of exception.
  - If that happens you don't want your actors to connect to that data source immediately because
  it might need time to recover and when it does recover you don't want your actress to suddenly
  start connecting all at once to your data source because it might bring it back down.
  - Which is why we implemented this back office supervisor pattern.
  */
  Thread.sleep(10000)
  system.terminate
}
