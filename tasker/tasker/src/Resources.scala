import java.util.concurrent.Executors

import cats.effect.{Blocker, IO, Resource}

object Resources {

  /**
    * An execution context that is safe to use for blocking operations wrapped into a Resource
    */
  val blockerResource: Resource[IO, Blocker] =
    Resource
      .make(IO(Executors.newCachedThreadPool()))(es => IO(es.shutdown()))
      .map(Blocker.liftExecutorService)

}
