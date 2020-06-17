package tasker

import cacher.CacherApp
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.app.DexThinService
import nl.surf.dex.figurer.FigurerApp
import runner.RunnerApp
import watcher.WatcherApp

object TaskerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- logger.info("Dexter started")
      runner <- DexThinService.thinService(RunnerApp, "runner").start(args)
      watcher <- DexThinService.thinService(WatcherApp, "watcher").start(args)
      cacher <- DexThinService.thinService(CacherApp, "cacher").start(args)
      figurer <- DexThinService.thinService(FigurerApp, "figurer").start(args)
      runnerExitCode <- List(runner, watcher, cacher, figurer)
        .map(_.join)
        .sequence
    } yield ExitCode(runnerExitCode.map(_.code).sum)
}
