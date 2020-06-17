package nl.surf.dex.figurer

import cats.Apply
import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.database.Permissions
import nl.surf.dex.database.config.{DbConf, DexTransactor}
import nl.surf.dex.figurer.ProgramStats.storeStats
import nl.surf.dex.figurer.messages.AnalyzeAlgorithm
import nl.surf.dex.figurer.program.PythonProgram
import nl.surf.dex.messaging.Messages.implicits._
import nl.surf.dex.messaging.QueueResources
import nl.surf.dex.messaging.config._
import nl.surf.dex.messaging.patterns.Direct
import nl.surf.dex.storage.owncloud.Webdav

object FigurerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  case class Deps(transactor: Transactor[IO],
                  msgConf: DexMessagingConf,
                  webdav: Webdav)

  private def handleMessages: Kleisli[IO, Deps, Unit] = {
    Kleisli { deps =>
      QueueResources.rabbitClientResource(deps.msgConf.broker).use {
        implicit rabbit =>
          rabbit.createConnectionChannel.use { implicit channel =>
            for {
              _ <- Direct.declareAndBind(deps.msgConf.analyze)
              consumer <- Direct
                .consumer[AnalyzeAlgorithm](deps.msgConf.analyze)
              _ <- consumer
                .evalMap(aa => {
                  for {
                    _ <- logger.debug("Handling a new message")
                    algorithmPath <- Permissions
                      .algorithmPath(aa.permission_id)
                      .run(deps.transactor)
                    _ <- logger.debug("Fetched algorithm info")
                    stats <- PythonProgram
                      .downloadedInTempR(algorithmPath)
                      .run(deps) // TODO: use natural transformation here?
                      .use(collectStats)
                    _ <- logger.debug("Downloaded algorithm")
                    eTag <- deps.webdav
                      .eTag(deps.webdav.webdavBase.change(algorithmPath))
                    _ <- logger.debug("Checked eTag")
                    _ <- storeStats(aa.permission_id, eTag, stats)
                      .run(deps.transactor) // Provide transactor
                  } yield ()
                })
                .compile
                .drain
            } yield IO(())
          }
      }
    }
  }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- logger.info("Figurer started")
      (msgConf, dbConf) <- Apply[IO]
        .product(DexMessagingConf.loadIO, DbConf.loadIO)
      transactor = DexTransactor.create(dbConf)
      webdav <- Webdav.makeClient
      _ <- handleMessages.run(Deps(transactor, msgConf, webdav))
    } yield ExitCode.Success
}
