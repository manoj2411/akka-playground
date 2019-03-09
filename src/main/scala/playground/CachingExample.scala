package playground

import scalacache._
import scalacache.guava._
import com.google.common.cache.CacheBuilder

import scala.util.{Failure, Success}

object CachingExample extends App {
  // We'll use the binary serialization codec - more on that later
//  import scalacache.serialization.binary._
  import scalacache.modes.try_._
  import scala.concurrent.duration._

  case class Cat(id: Int, name: String)
  def fetch(name: String) = {
    get(name) match {
      case Success(succValue) =>
        succValue match {
          case Some(value) =>
            print(s"cache hit [$name]: ")
            value
          case None =>
            print(s"cache miss [$name]: ")
            None
        }
      case Failure(exception) =>
        None
    }
  }


  val underlyingGuavaCache = CacheBuilder.newBuilder().maximumSize(1000L).build[String, Entry[Cat]]
  implicit val guavaCache: Cache[Cat] = GuavaCache(underlyingGuavaCache)

  val ericTheCat = Cat(1, "Eric")
  val doraemon = Cat(99, "Doraemon")
  println(put("eric")(ericTheCat, ttl = Some(1.seconds)))

  println(fetch("eric"))
  Thread.sleep(1000)
  println(fetch("eric"))
}
