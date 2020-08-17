package tasker

import shares.SharesApp
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.figurer.FigurerApp
import runner.RunnerApp
import watcher.WatcherApp

object DexterApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- logger.info("Dexter started")
      codes <- List(RunnerApp, SharesApp, WatcherApp, FigurerApp)
        .map(_.run(args))
        .parSequence
    } yield ExitCode(codes.map(_.code).sum)
}
