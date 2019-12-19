package cacher.conf
import scala.concurrent.duration._
object CacherConfig {

  object server {
    object timeouts {
      val connection: FiniteDuration = 20.seconds
      val idle: FiniteDuration = 5.minutes
      val request: FiniteDuration = 4.minutes
      val responseHeader: FiniteDuration = 3.minutes
    }
  }

  object client {
    val idleTimeout: FiniteDuration = 5.minutes
  }

  object update {
    val initialInterval: FiniteDuration = 1.second
    val ceilingInterval: FiniteDuration = 1.hour
  }

}
