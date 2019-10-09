package part1recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

// 2.
object MultiThreading extends App {

  val aThread = new Thread(new Runnable {
    override def run(): Unit = println("In the thread")
  })
  // Scala lambda syntax
  val anotherThread = new Thread(() => println("From another thread"))
  aThread.start
  anotherThread.start
  anotherThread.join // waits for thread to finish
  aThread.join

  /*
    We can do lot of parallel work with threads but they are **unpredictable**
   */
  class BankAccount(private var amount: Int) {
    def withdraw(money: Int) = this.amount -= money
    def safeWithdraw(money: Int) = this.synchronized {
      this.amount -= money
    }
  }
  /* BankAccount(500)

    T1 -> withdraw(100)
    T2 -> withdraw(200)

    - Standard failures (example) can be 2 withdraw transactions on a account bcz
        this.amount = this.amount - 1000 is NOT ATOMIC

    - Standard solutions is by adding Synchronise blocks. This soln becomes difficult in big apps

    - Another solution is to add @volatile on the private member like:
        BankAccount(@volatile private var amount: Int)
      - Its locks both reads and writes of this member
      - But only works with primitive types.

    - Inter-thread communication on JVM
        done via wait-notify mechanism.
   */

  /* Scala Futures
    - will be evaluated on different thread.
    - Futures comes with Callbacks like onComplete
   */
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future { 42 }
  future.onComplete {
    case Success(42) => println("life is awesome")
    case Failure(_) => println("Something happened to life")
  }
  // FP: Future is monadic construct, it has functional primitives
  // Future supports - map, flatMap, filter, for comprehension etc.  andThen, recover/recoverWith


  /* Problems with existing multi-threading model
  1. OOP encapsulation is only valid in single threaded env (Video #6 for examples)
      a. synchronization & locks will solve it but can result in deadlock, livelock etc and they are slow.
    We need a DataStructure which is:
      a. Fully encapsulated in multi threaded env / distributed env
      b. with no locks
  2. Delegating something to a thread(delegate task to bg) is a PAIN
    You have a running thread and you want to pass a runnable to that thread. Producer Consumer problem
    Other big problems:
      a. Send other signals like run this every n secs
      b. Multiple bg tasks, how to identify which thread gets which task
      c. From running thread how you identify who gave you the signal
      d. What if the bg thread stuck or throw exception or crash
    We need a Data Structure
      a. can safely receive any kind of signal/message
      b. can identify who gave that signal
      c. is itself easily identifiable
      d. can guard against failure
   3. Tracking and dealing with errors in multi threaded(distributed) env is PainInTheA

   ----

  Actors: a new way of thinking and modeling the code
    - traditional object: we model code around classes and instances, each instance has its own data and interact with the world via methods
    - Actors are similar as they also have private data but the interaction with the world is different
     - it happens by sending messages to them, asynchronously
     - **Actors are objects which we can't access directly but can only send messages**
     -
  */

  // 1M numbers b/w 10 threads
  val futures = (0 to 9)
    .map(i => 100000 * i to 100000 * (i + 1))
    .map(range => Future {
      if (range.contains(241234)) throw new RuntimeException("random error")
      range.sum
    })

  val sumFuture = Future.reduceLeft(futures)(_ + _)

  sumFuture.onComplete(println)
  Thread.sleep(1000)
}
