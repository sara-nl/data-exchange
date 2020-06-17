package nl.surf.dex.messaging.config

import cats.effect.{Blocker, ContextShift, IO}
import nl.surf.dex.config.DexConfig

object DexMessagingConf extends DexConfig("messaging") {

  def loadIO(implicit cs: ContextShift[IO]): IO[DexMessagingConf] = {
    import pureconfig.generic.auto._
    Blocker[IO].use(configSrc.loadF[IO, DexMessagingConf])
  }

}

case class DexMessagingConf(broker: BrokerConf,
                            todo: QueueConf,
                            done: QueueConf,
                            analyze: QueueConf)
