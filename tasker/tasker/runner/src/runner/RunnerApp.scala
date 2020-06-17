package runner

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.messaging.QueueResources
import nl.surf.dex.messaging.config.DexMessagingConf
import nl.surf.dex.messaging.patterns.Direct
import nl.surf.dex.storage.config.DexStorageConf
import nl.surf.dex.messaging.Messages.{StartContainer, TaskProgress}

object RunnerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  private def handleMessages(storageConf: DexStorageConf,
                             runnerConf: RunnerConf,
                             messagingConf: DexMessagingConf) =
    QueueResources.rabbitClientResource(messagingConf.broker).use {
      implicit rabbit =>
        rabbit.createConnectionChannel.use { implicit channel =>
          import nl.surf.dex.messaging.Messages.implicits._
          for {
            _ <- Direct.declareAndBind(messagingConf.todo)
            consumer <- Direct.consumer[StartContainer](messagingConf.todo)
            _ <- Direct.declareAndBind(messagingConf.done)
            publisher <- Direct.publisher[TaskProgress](messagingConf.done)
            _ <- new SecureContainerFlow(
              consumer,
              publisher,
              storageConf.webdavBase,
              runnerConf.docker
            ).flow.compile.drain
          } yield ()
        }
    }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- logger.info("Runner started")
      storageConf <- DexStorageConf.loadIO
      messagingConf <- DexMessagingConf.loadIO
      conf <- RunnerConf.loadIO
      _ <- handleMessages(storageConf, conf, messagingConf)
    } yield ExitCode.Success
}
