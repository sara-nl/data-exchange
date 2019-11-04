package tasker.queue

import cats.effect.{ContextShift, IO}
import dev.profunktor.fs2rabbit.interpreter.Fs2Rabbit
import tasker.concurrency.ConcurrencyResources
import tasker.config.TaskerConfig

object QueueResources {

  def rabbitClientResource(implicit cs: ContextShift[IO]) =
    ConcurrencyResources.blocker.evalMap { blocker =>
      Fs2Rabbit[IO](TaskerConfig.rabbitConfig, blocker)
    }

}
