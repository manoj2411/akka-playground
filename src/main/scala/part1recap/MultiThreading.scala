package part1recap

object MultiThreading extends App {

  val aThread = new Thread(new Runnable {
    override def run(): Unit = println("In the thread")
  })
  // Scala lambda syntax
  val anotherThread = new Thread(() => println("From another thread"))
  aThread.start
  anotherThread.start
  anotherThread.join
  aThread.join

  /*
    We can do lot of parallel work with threads but they are unpredictable
    - Standard failures (example) can be 2 withdraw transactions on a account bcz
        this.amount = this.amount - 1000 is NOT ATOMIC
    - Standard solutions is by adding Synchronise blocks. This soln becomes difficult in big apps
   Inter-thread communication on JVM via wait-notify mechanism.

   Scala Futures
    - will be evaluated on different thread.
    - Futures comes with Callbacks like onComplete
      future.onComplete {
        case Success(42) => println("life is awesome")
        case Failure(_) => println("Something happened to life")
      }
    - Future supports - map, flatMap, filter etc. andThen, recoverWith

  -------------------------------------

  1. OOP encapsulation is only valid in single threaded env
      a. synchronization & locks will solve it but can result in deadlock, livelock etc
    We need a DataStructure which is:
      a. Fully encapsulated in multi threaded env / distributed env
      b. with no locks
  2. Delegating something to a thread(delegate task to bg) is a PAIN
    You have a running thread and you want to pass a runnable to that thread. Producer Consumer problem
    Other big problems:
      a. Send other signals like run this every n secs
      b. Multiple bg tasks, how to identify which thread gets which task
      c. From running thread how you identify who gave you the signal
      d. What is the bg thread stuck or throw exception or crash
    We need a Data Structure
      a. can safely receive any kind of signal/message
      b. can identify who gave that signal
      c. is itself easily identifiable
      d. can guard against failure
   3. Tracking and dealing with errors in multi threaded(distributed) env is PITA

   ----

  Actors: a new way of thinking and modeling the code
    - traditional object: we model code around classes and instances, each instance has its own data and interact with the world via methods
    - Actors are similar as they also have private data but the interaction with the world is different
     - it happens by sending messages to them, asynchronously
     - **Actors are objects which we can't access directly but can only send messages**
     -


  */


}
