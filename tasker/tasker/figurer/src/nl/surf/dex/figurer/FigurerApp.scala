package nl.surf.dex.figurer

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.database.Permissions
import nl.surf.dex.database.config.{DbConf, DexTransactor}
import nl.surf.dex.figurer.ProgramStats.storeStats
import nl.surf.dex.figurer.messages.AnalyzeAlgorithm
import nl.surf.dex.figurer.program.PythonProgram
import nl.surf.dex.messaging.QueueResources
import nl.surf.dex.messaging.config._
import nl.surf.dex.messaging.patterns.Direct
import nl.surf.dex.storage.local.LocalFS
import nl.surf.dex.storage.multi.DexFileset
object FigurerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  case class Deps(transactor: Transactor[IO], msgConf: DexMessagingConf)

  private def handleMessages: Kleisli[IO, Deps, Unit] = {
    val tmpDirR = LocalFS.tempDir("figurer".some)
    Kleisli { deps =>
      QueueResources.rabbitClientResource(deps.msgConf.broker).use { implicit rabbit =>
        rabbit.createConnectionChannel.use { implicit channel =>
          import io.circe.generic.auto._
          for {
            _ <- Direct.declareAndBind(deps.msgConf.analyze)
            consumer <-
              Direct
                .consumer[AnalyzeAlgorithm](deps.msgConf.analyze)
            _ <-
              consumer
                .evalMap(command => {
                  for {
                    _ <- logger.debug(s"Handling a new message: $command")
                    algorithmLocation <-
                      Permissions
                        .algorithmLocation(command.permission_id)
                        .run(deps.transactor)
                    (hash, stats) <- (for {
                        tempDir <- tmpDirR
                        filesetOps <-
                          DexFileset
                            .forStorage(algorithmLocation.storage)
                        entryFile <- Resource.liftF(
                          filesetOps
                            .copySharedFileset(algorithmLocation.path, tempDir)
                        )
                        hash <- Resource.liftF(
                          filesetOps.getFilesetHash(algorithmLocation.path)
                        )
                        pythonProgram <- Resource.liftF(PythonProgram(entryFile))
                        stats <- Resource.liftF(collectStats(pythonProgram))
                      } yield (hash, stats)).use(IO.pure)
                    _ <-
                      logger
                        .debug(s"Algorithm analysis completed. Hash: $hash")
                    _ <- storeStats(command.permission_id, hash, stats)
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
      (msgConf, dbConf) <- (DexMessagingConf.loadIO, DbConf.loadIO).parTupled
      transactor = DexTransactor.create(dbConf)
      _ <- handleMessages.run(Deps(transactor, msgConf))
    } yield ExitCode.Success
}
