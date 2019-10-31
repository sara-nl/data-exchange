package watcher

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.config.TaskerConfig

object WatcherApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    logger.info("Watcher started") *>
      fs2.Stream
        .awakeEvery[IO](TaskerConfig.watcher.awakeInterval)
        .map(_ => ())
        .through(DataSet.newDatasetsPipe)
        .evalTap { ds =>
          logger.info(s"Processing a new dataset $ds")
        }
        .compile
        .drain *>
      IO(ExitCode.Success)
}
