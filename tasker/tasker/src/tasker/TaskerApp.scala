package tasker

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import watcher.WatcherApp
import runner.RunnerApp

object TaskerApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- IO(println("Tasker started"))
      runner <- RunnerApp.run(Nil).start
      watcher <- WatcherApp.run(Nil).start
      runnerExitCode <- runner.join
      watcherExitCode <- watcher.join
    } yield ExitCode(runnerExitCode.code + watcherExitCode.code)
}
