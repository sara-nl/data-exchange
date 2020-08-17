package tasker.concurrency

import java.util.concurrent.Executors

import cats.effect.{Blocker, ContextShift, IO, Resource, Timer}
import io.netty.util.concurrent.DefaultThreadFactory

import scala.concurrent.ExecutionContext.fromExecutor

object ConcurrencyResources {

  object implicits {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val ctxShiftGlobal: ContextShift[IO] = IO.contextShift(global)
  }

  /**
    * An execution context that is safe to use for blocking operations wrapped into a Resource
    */
  val blocker: Resource[IO, Blocker] =
    Resource
      .make(IO(Executors.newCachedThreadPool()))(es => IO(es.shutdown()))
      .map(Blocker.liftExecutorService)

  def newCachedTPContextShift(label: String): ContextShift[IO] =
    IO.contextShift(
      fromExecutor(
        Executors.newCachedThreadPool(new DefaultThreadFactory(label, true))
      )
    )

  def newFixedContextShift(label: String): ContextShift[IO] = IO.contextShift(
    fromExecutor(
      Executors.newFixedThreadPool(10, new DefaultThreadFactory(label, true))
    )
  )

  def newTimer(label: String): Timer[IO] = IO.timer(
    fromExecutor(
      Executors
        .newFixedThreadPool(3, new DefaultThreadFactory(label, true))
    )
  )
}
