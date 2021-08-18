package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
//import part2actors.ActorCapabilities.BankAccount.{Deposit, Withdraw}



object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi!" => sender() ! "Hello there!" // replying to a message
      case message: String => println(s"[$self] I have received ${message}")
      case number: Int => println(s"[simple actor] I have received a NUMBER: $number")
      case SpecialMessage(contents) => println(s"[simple actor] I have received something special: $contents")
      case SendMessageToYourself(content) =>
        self ! content
      case SayHiTo(ref) => ref ! "Hi!" // alice is being passed as a sender
      case WirelessPhoneMessage(content, ref) => ref forward (content + "s") // keep the original sender of the WPM
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"

  // 1 - message can be of any type
  // a) messages must be IMMUTABLE
  // b) messages must be SERIALIZABLE
  // in practice use case classes and case objects

  simpleActor ! 42 // who is the sender?

  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("some special content")

  // 2 - actors have information about their context and about themselves
  // context.self === `this` in OOP

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and Im proud of it!")

  // 3 - actors can REPLY to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)


  // 4 - dead letters
  alice ! "Hi" // reply to "me"

  // 5 - forwarding messages
  // D -> A -> B
  // forwarding = sending a message with the ORIGINAL SENDER
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi", bob) // noSender

  /*
  1. a Counter actor
    - Increment
    - Decrement
    - Print
   */

  class Counter extends Actor {
    var count = 0

    override def receive: Receive = {
      case "Increment" => count += 1
        println("[counter] received increment command")
      case "Decrement" => count -= 1
        println("[counter] received decrement command")
      case "Print" => println(s"[counter] current count is: $count")
    }
  }

  val countActor = system.actorOf(Props[Counter], "CounterActor")

  countActor ! "Decrement"
  countActor ! "Increment"
  countActor ! "Increment"

  countActor ! "Print"

  // Implementation with an case object

  // DOMAIN of the counter
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class NewCounter extends Actor {
    import Counter._
    var count = 0

    override def receive: Receive = {
      case Increment => count += 1
        println("[counter] received increment command")
      case Decrement => count -= 1
        println("[counter] received decrement command")
      case Print => println(s"[counter] current count is: $count")
    }
  }

  val newCounter = system.actorOf(Props[NewCounter], "betterCounter")

  (1 to 5).foreach(_ => newCounter ! Counter.Increment)
  (1 to 2).foreach(_ => newCounter ! Counter.Decrement)
  newCounter ! Counter.Print
  /*
  2. Bank account as an actor
    receives
    - Deposit amount
    - Withdraw amount
    - Statement

    replies with
    - Success/Failure

    interact with some other kind of actor
   */


  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(message: String)
  }

  class BankAccount extends Actor {
    import BankAccount._
    var currentAmount = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TransactionFailure("invalid deposit number")
        else {
          currentAmount += amount
          sender() ! TransactionSuccess(s"successfully deposited $amount")
        }
      case Withdraw(amount) => {
        if (amount < 0) sender() ! TransactionFailure("invalid withdraw number")
        else if (currentAmount - amount > 0) {
          currentAmount -= amount
          sender() ! TransactionSuccess("successfully withdrew amount")
        }
        else sender() ! TransactionFailure("insufficient funds")
      }
      case Statement => sender() ! s"Your balance is $currentAmount"

    }
  }

  object Terminal {
    case class LiveTheLife(account: ActorRef)
  }
  class Terminal extends Actor {
    import Terminal._
    import BankAccount._

    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(90000)
        account ! Withdraw(500)
        account ! Statement
      case message => println(message.toString)
    }
  }

    val account = system.actorOf(Props[BankAccount], "BankAccount")
    val terminal = system.actorOf(Props[Terminal], "Terminal")

    import Terminal._ // interesting part, does not work without it
    terminal ! LiveTheLife(account)

}
