package runner

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.messaging.Messages.{StartContainer, TaskProgress}
import nl.surf.dex.messaging.QueueResources
import nl.surf.dex.messaging.config.DexMessagingConf
import nl.surf.dex.messaging.patterns.Direct
import nl.surf.dex.storage.multi.DexFileset
import runner.SecureContainerFlow.Deps
object RunnerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- logger.info("Runner started")
      (messagingConf, conf) <- (DexMessagingConf.loadIO, RunnerConf.loadIO).parTupled
      _ <- QueueResources.rabbitClientResource(messagingConf.broker).use {
        implicit rabbit =>
          rabbit.createConnectionChannel.use { implicit channel =>
            for {
              _ <- Direct.declareAndBind(messagingConf.todo)
              consumer <- {
                import nl.surf.dex.storage.CloudStorage.codec._
                Direct.consumer[StartContainer](messagingConf.todo)
              }
              _ <- Direct.declareAndBind(messagingConf.done)
              publisher <- {
                import TaskProgress.codecs._
                Direct.publisher[TaskProgress](messagingConf.done)
              }
              _ <- new SecureContainerFlow(
                Deps(consumer, publisher, DexFileset.forStorage, conf.docker)
              ).flow.compile.drain
            } yield ()
          }
      }
    } yield ExitCode.Success
}
