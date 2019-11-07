package tasker.concurrency

import java.util.concurrent.Executors

import cats.effect.{Blocker, IO, Resource}

object ConcurrencyResources {

  /**
    * An execution context that is safe to use for blocking operations wrapped into a Resource
    */
  val blocker: Resource[IO, Blocker] =
    Resource
      .make(IO(Executors.newCachedThreadPool()))(es => IO(es.shutdown()))
      .map(Blocker.liftExecutorService)
}
