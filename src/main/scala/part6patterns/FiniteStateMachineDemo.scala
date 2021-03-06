package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._



class VendingMachineSpec extends TestKit(ActorSystem("afmTest"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

    override def afterAll(): Unit = {
      TestKit.shutdownActorSystem(system)
    }
    import VendingMachine._

    "VendingMachine" should {

      "send error when not initialised" in {
        val vendingMachine = system.actorOf(Props[VendingMachine])
        vendingMachine ! RequestItem

        expectMsg(Error("not ready to use"))
      }

      "report invalid item" in {
        val vendingMachine = system.actorOf(Props[VendingMachine])
        vendingMachine ! Init(Map(), Map())
        vendingMachine ! RequestItem("coke")

        expectMsg(Error("invalid item"))
      }

      "report item out of stock" in {
        val vendingMachine = system.actorOf(Props[VendingMachine])
        vendingMachine ! Init(Map(), Map("coke" -> 0))
        vendingMachine ! RequestItem("coke")

        expectMsg(Error("out of stock"))
      }

      "throw timeout after 1 sec when no money received" in {
        val vendingMachine = system.actorOf(Props[VendingMachine])
        vendingMachine ! Init(Map("coke" -> 2), Map("coke" -> 2))
        vendingMachine ! RequestItem("coke")
        expectMsg(Instruction("Please insert 2 dollars"))

        within(1.5 second) {
          expectMsg(Error("timeout"))
        }
      }

      "receive money back when timeout" in {
        val vendingMachine = system.actorOf(Props[VendingMachine])
        vendingMachine ! Init(Map("coke" -> 2), Map("coke" -> 10))
        vendingMachine ! RequestItem("coke")
        expectMsg(Instruction("Please insert 2 dollars"))
        vendingMachine ! ReceiveMoney(1)
        expectMsg(Instruction("Please insert 1 dollars"))

        within(1.5 second) {
          expectMsg(GiveChange(1))
          expectMsg(Error("timeout"))
        }
      }

      "receive item and change when send all money" in {
        val vendingMachine = system.actorOf(Props[VendingMachine])

        vendingMachine ! Init(Map("coke" -> 2), Map("coke" -> 10))
        vendingMachine ! RequestItem("coke")
        expectMsg(Instruction("Please insert 2 dollars"))
        vendingMachine ! ReceiveMoney(2)

        expectMsg(DeliverItem("coke"))
      }

      "receive item and change when send more money" in {
        val vendingMachine = system.actorOf(Props[VendingMachine])

        vendingMachine ! Init(Map("coke" -> 2), Map("coke" -> 10))
        vendingMachine ! RequestItem("coke")
        expectMsg(Instruction("Please insert 2 dollars"))
        vendingMachine ! ReceiveMoney(5)

        expectMsg(DeliverItem("coke"))
        expectMsg(GiveChange(3))
      }

      "receive item and change when send money by more than 1 message" in {
        val vendingMachine = system.actorOf(Props[VendingMachine])

        vendingMachine ! Init(Map("coke" -> 3), Map("coke" -> 10))
        vendingMachine ! RequestItem("coke")
        expectMsg(Instruction("Please insert 3 dollars"))
        vendingMachine ! ReceiveMoney(2)
        expectMsg(Instruction("Please insert 1 dollars"))
        vendingMachine ! ReceiveMoney(2)

        expectMsg(DeliverItem("coke"))
        expectMsg(GiveChange(1))
      }

    }

  "VendingMachine FSM" should {

    "send error when not initialised" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! RequestItem

      expectMsg(Error("not ready to use"))
    }

    "report invalid item" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Init(Map(), Map())
      vendingMachine ! RequestItem("coke")

      expectMsg(Error("invalid item"))
    }

    "report item out of stock" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Init(Map(), Map("coke" -> 0))
      vendingMachine ! RequestItem("coke")

      expectMsg(Error("out of stock"))
    }

    "throw timeout after 1 sec when no money received" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Init(Map("coke" -> 2), Map("coke" -> 2))
      vendingMachine ! RequestItem("coke")
      expectMsg(Instruction("Please insert 2 dollars"))

      within(1.5 second) {
        expectMsg(Error("timeout"))
      }
    }

    "receive money back when timeout" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Init(Map("coke" -> 2), Map("coke" -> 10))
      vendingMachine ! RequestItem("coke")
      expectMsg(Instruction("Please insert 2 dollars"))
      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction("Please insert 1 dollars"))

      within(1.5 second) {
        expectMsg(GiveChange(1))
        expectMsg(Error("timeout"))
      }
    }

    "receive item and change when send all money" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])

      vendingMachine ! Init(Map("coke" -> 2), Map("coke" -> 10))
      vendingMachine ! RequestItem("coke")
      expectMsg(Instruction("Please insert 2 dollars"))
      vendingMachine ! ReceiveMoney(2)

      expectMsg(DeliverItem("coke"))
    }

    "receive item and change when send more money" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])

      vendingMachine ! Init(Map("coke" -> 2), Map("coke" -> 10))
      vendingMachine ! RequestItem("coke")
      expectMsg(Instruction("Please insert 2 dollars"))
      vendingMachine ! ReceiveMoney(5)

      expectMsg(DeliverItem("coke"))
      expectMsg(GiveChange(3))
    }

    "receive item and change when send money by more than 1 message" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])

      vendingMachine ! Init(Map("coke" -> 3), Map("coke" -> 10))
      vendingMachine ! RequestItem("coke")
      expectMsg(Instruction("Please insert 3 dollars"))
      vendingMachine ! ReceiveMoney(2)
      expectMsg(Instruction("Please insert 1 dollars"))
      vendingMachine ! ReceiveMoney(2)

      expectMsg(DeliverItem("coke"))
      expectMsg(GiveChange(1))
    }

  }
}


object VendingMachine {

  case class Init(prices: Map[String, Int], stock: Map[String, Int])
  case class RequestItem(item: String)

  case class Instruction(message: String)
  case class ReceiveMoney(amount: Int)
  case class Error(reason: String)
  case class GiveChange(amount: Int)

  case class DeliverItem(item: String)
  case class Timeout(originalSender: ActorRef)
}

class VendingMachine extends Actor with ActorLogging {
  import VendingMachine._
  implicit val ex: ExecutionContext = context.dispatcher

  def receive: Receive = idle

  def idle: Receive = {
    case Init(prices, stock) =>
      log.info("Initializing")
      context.become(ready(prices, stock))
    case _ =>
      sender ! Error("not ready to use")
  }

  def ready(prices: Map[String, Int], stock: Map[String, Int]): Receive = {
    case RequestItem(item) => stock.get(item) match {
      case Some(0) =>
        sender ! Error("out of stock")
      case None =>
        sender ! Error("invalid item")
      case Some(_) =>
        val price = prices(item)
        sender ! Instruction(s"Please insert $price dollars")
        context.become(waitingForMoney(prices, stock, item, scheduleTimeout(sender), 0))
    }
  }

  def waitingForMoney(prices: Map[String, Int], stock: Map[String, Int], item: String, timeout: Cancellable, amount: Int): Receive = {
    case ReceiveMoney(currAmount) if amount + currAmount < prices(item) =>
      timeout.cancel()
      val moneyReceived = amount + currAmount
      val remainingMoney = prices(item) - moneyReceived
      sender ! Instruction(s"Please insert $remainingMoney dollars")
      context.become(waitingForMoney(prices, stock, item, scheduleTimeout(sender), moneyReceived))
    case ReceiveMoney(currAmount) =>
      timeout.cancel()
      val newStock = stock + (item -> (stock(item) - 1))
      val receivedMoney = amount + currAmount
      sender ! DeliverItem(item)
      if (receivedMoney > prices(item))
        sender ! GiveChange(receivedMoney - prices(item))
      context.become(ready(prices, newStock))
    case Timeout(originalSender) =>
      log.info("timeout!")
      if (amount > 0 )
        originalSender ! GiveChange(amount)
      originalSender ! Error("timeout")
      context.become(ready(prices, stock))
  }

  private def scheduleTimeout(originalSender: ActorRef): Cancellable =
    context.system.scheduler.scheduleOnce(1 second) {
      self ! Timeout(originalSender)
    }

}

// Step1: Define states and data of the Actor

//  states
trait VendingState
case object NotReady extends VendingState
case object Ready extends VendingState
case object WaitingForMoney extends VendingState

// Date
trait VendingData
case object NotReadyData extends VendingData
case class ReadyData(prices: Map[String, Int], stock: Map[String, Int]) extends VendingData
case class WaitingForMoneyData(prices: Map[String, Int],
                               stock: Map[String, Int],
                               item: String,
                               requester: ActorRef,
                               amount: Int
                              ) extends VendingData

/*
   AFM - an extension pf actor with some logic & methods from Akka
          - The way of thinking in AFM is different, we dont handle messages directly
          - We dont have a receive handler
       - When an FSM receives a message it triggers an Event
       Event(msg, data)
       - Now we need to handle States & Events, not the messages
*/

class VendingMachineFSM extends FSM[VendingState, VendingData] {
  import VendingMachine._

  // we need to handle States & Events, not the messages

  startWith(NotReady, NotReadyData)

  when(NotReady) {
    case Event(Init(prices, stock), NotReadyData) =>
      goto(Ready) using ReadyData(prices, stock)
      // === context.become(ready(prices, stock))
    case _ =>
      sender ! Error("not ready to use")
      stay()
  }

  when(Ready) {
    case Event(RequestItem(item), ReadyData(prices, stock)) => stock.get(item) match {
      case Some(0) =>
        sender ! Error("out of stock")
        stay()
      case None =>
        sender ! Error("invalid item")
        stay()
      case Some(_) =>
        val price = prices(item)
        sender ! Instruction(s"Please insert $price dollars")
        // context.become(waitingForMoney(prices, stock, item, scheduleTimeout(sender), 0))
        goto(WaitingForMoney) using WaitingForMoneyData(prices, stock, item, sender, 0)
    }

  }

  when(WaitingForMoney, stateTimeout = 1 second) {
    case Event(StateTimeout, WaitingForMoneyData(prices, stock, item, requester, amount)) =>
      log.info("timeout!")
      if (amount > 0 )
        requester ! GiveChange(amount)
      requester ! Error("timeout")
      // context.become(ready(prices, stock))
      goto(Ready) using ReadyData(prices, stock)
    case Event(ReceiveMoney(currAmount), WaitingForMoneyData(prices, stock, item, requester, amount)) if amount + currAmount < prices(item) =>
      val moneyReceived = amount + currAmount
      val remainingMoney = prices(item) - moneyReceived
      requester ! Instruction(s"Please insert $remainingMoney dollars")
      // context.become(waitingForMoney(prices, stock, item, scheduleTimeout(sender), moneyReceived))
      stay() using part6patterns.WaitingForMoneyData(prices, stock, item, requester, moneyReceived)
    case Event(ReceiveMoney(currAmount), WaitingForMoneyData(prices, stock, item, requester, amount)) =>
      val newStock = stock + (item -> (stock(item) - 1))
      val receivedMoney = amount + currAmount
      requester ! DeliverItem(item)
      if (receivedMoney > prices(item))
        requester ! GiveChange(receivedMoney - prices(item))
      // context.become(ready(prices, newStock))
      goto(Ready) using ReadyData(prices, newStock)
  }

  whenUnhandled {
    case Event(_, _) =>
      sender() ! Error("command not found")
      stay()
  }

  onTransition {
    case stateA -> stateB => log.info(s"changing from $stateA to $stateB")
  }

  initialize() // IMP: Starts the chain of handlers
}

