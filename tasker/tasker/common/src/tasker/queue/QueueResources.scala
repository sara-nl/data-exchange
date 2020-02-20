package tasker.queue

import cats.effect.{ContextShift, IO, _}
import dev.profunktor.fs2rabbit.interpreter.Fs2Rabbit
import tasker.concurrency.ConcurrencyResources
import tasker.config.CommonConf.RabbitmqConf

object QueueResources {

  import tasker.config.conversions._

  def rabbitClientResource(
    conf: RabbitmqConf
  )(implicit cs: ContextShift[IO]): Resource[IO, Fs2Rabbit[IO]] =
    ConcurrencyResources.blocker.evalMap { blocker =>
      Fs2Rabbit[IO](conf.fs2RabbitConfig, blocker)
    }

}
