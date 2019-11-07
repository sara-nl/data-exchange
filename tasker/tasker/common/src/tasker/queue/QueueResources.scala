package tasker.queue

import cats.effect.{ContextShift, IO}
import dev.profunktor.fs2rabbit.interpreter.Fs2Rabbit
import tasker.concurrency.ConcurrencyResources
import tasker.config.TaskerConfig

import cats.effect._

object QueueResources {

  def rabbitClientResource(
    implicit cs: ContextShift[IO]
  ): Resource[IO, Fs2Rabbit[IO]] =
    ConcurrencyResources.blocker.evalMap { blocker =>
      Fs2Rabbit[IO](TaskerConfig.rabbitConfig, blocker)
    }

}
