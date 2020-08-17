package watcher

import cats.effect.{ExitCode, IO, IOApp, Timer}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.database.config.{DbConf, DexTransactor}
import nl.surf.dex.messaging.Messages.StartContainer
import nl.surf.dex.messaging.QueueResources.rabbitClientResource
import nl.surf.dex.messaging.config.DexMessagingConf
import nl.surf.dex.messaging.patterns.Direct
import nl.surf.dex.storage.multi.DexFileset
import tasker.concurrency.ConcurrencyResources

object WatcherApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override protected implicit def timer: Timer[IO] =
    ConcurrencyResources.newTimer("watcher-timer")

  private def todoPublisherResource(conf: DexMessagingConf) = {
    rabbitClientResource(conf.broker).flatMap { implicit rabbit =>
      rabbit.createConnectionChannel.evalMap { implicit channel =>
        import nl.surf.dex.storage.CloudStorage.codec._
        Direct.declareAndBind(conf.todo) *>
          Direct.publisher[StartContainer](conf.todo)
      }
    }
  }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- WatcherConf.loadIO
      messagingConf <- DexMessagingConf.loadIO
      dbConf <- DbConf.loadIO
      _ <- logger.info(s"Watcher started: (interval ${config.awakeInterval})")
      _ <- todoPublisherResource(messagingConf).use { publisher =>
        Watcher.scheduled.run(
          Watcher.Deps(
            fs2.Stream.awakeEvery[IO](config.awakeInterval),
            DexFileset.forStorage,
            publisher,
            DexTransactor.create(dbConf)
          )
        )
      }
    } yield ExitCode.Success
}
