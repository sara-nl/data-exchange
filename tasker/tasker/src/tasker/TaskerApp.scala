package tasker

import java.util.concurrent.Executors

import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.netty.util.concurrent.DefaultThreadFactory
import watcher.WatcherApp
import runner.RunnerApp
import tasker.config.TaskerConfig

object TaskerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- logger.info("Tasker started")
      runner <- RunnerApp
        .run(Nil)
        .start(TaskerConfig.concurrency.newCachedTPContextShift("runner"))
      watcher <- WatcherApp
        .run(Nil)
        .start(TaskerConfig.concurrency.newCachedTPContextShift("watcher"))
      runnerExitCode <- runner.join
      watcherExitCode <- watcher.join
    } yield ExitCode(runnerExitCode.code + watcherExitCode.code)
}
