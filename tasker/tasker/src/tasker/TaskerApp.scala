package tasker

import cacher.CacherApp
import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import runner.RunnerApp
import tasker.config.TaskerConfig
import watcher.WatcherApp

object TaskerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- logger.info("Tasker started")
      runner <- RunnerApp
        .run(Nil)
        .start(TaskerConfig.concurrency.newFixedContextShift("runner"))
      watcher <- WatcherApp
        .run(Nil)
        .start(TaskerConfig.concurrency.newFixedContextShift("watcher"))
      cacher <- CacherApp
        .run(Nil)
        .start(TaskerConfig.concurrency.newFixedContextShift("cacher"))
      runnerExitCode <- runner.join
      watcherExitCode <- watcher.join
      cacherExitCode <- cacher.join
    } yield
      ExitCode(runnerExitCode.code + watcherExitCode.code + cacherExitCode.code)
}
