package tasker

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import watcher.WatcherApp
import runner.RunnerApp

object TaskerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- logger.info("Tasker started")
      runner <- RunnerApp.run(Nil).start
      watcher <- WatcherApp.run(Nil).start
      runnerExitCode <- runner.join
      watcherExitCode <- watcher.join
    } yield ExitCode(runnerExitCode.code + watcherExitCode.code)
}
