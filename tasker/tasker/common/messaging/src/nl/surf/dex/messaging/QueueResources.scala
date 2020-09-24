package nl.surf.dex.messaging

import cats.effect.{ContextShift, IO, Resource}
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import nl.surf.dex.messaging.config.BrokerConf
import tasker.concurrency.ConcurrencyResources

object QueueResources {

  def rabbitClientResource(
      conf: BrokerConf
  )(implicit cs: ContextShift[IO]): Resource[IO, RabbitClient[IO]] =
    ConcurrencyResources.blocker.evalMap { blocker =>
      RabbitClient[IO](conf.fs2RabbitConfig, blocker)
    }

}
