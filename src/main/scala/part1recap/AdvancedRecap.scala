package part1recap

// 1.
object AdvancedRecap extends App {
  // Partial Functions
  val aParitlaFunction: PartialFunction[Int, String]  = {
    case 1 => "One"
    case 2 => "Two"
    case 42 => "Meaning of life"
  }

  val pf = (x: Int) => x match {
    case 1 => "One"
    case 2 => "Two"
    case 42 => "Meaning of life"
  }
  // These both are equivalent Partial Functions.
  // PF are based on PM

  // LIFTING
  val lifted = aParitlaFunction.lift // turns PF into a total function from Int => Option[String]
  println(lifted(2)) // => Some("Two")
  println(lifted(4)) // => None

  // orElse can be used to compose/chain/add cases to PF

  // type aliases, name the types of complex type and use the new type
  type ReceiveType = PartialFunction[Any, Unit]
  type StatusCode = Map[Int, String]

  // implicits

  // implicit conversions, multiple ways
  // 1. implicit defs
    case class Person(name: String) {
      def greet = s"hey from $name"
    }
    implicit def stringToPerson(name: String): Person = Person(name)
    println("Bob".greet)

  // 2. implicit classes
  implicit class EnrichedString(string: String) {
    def meaningOfLife = 42
  }
  println("".meaningOfLife)

}
