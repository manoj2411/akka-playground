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
}
