package watcher

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import scala.concurrent.duration._

object WatcherApp extends IOApp {

  def doRepeatedly(action: IO[Unit]): IO[Unit] =
    action >> IO.sleep(5.seconds) >> IO.suspend(doRepeatedly(action))

  val sendTasksForNewFiles: IO[Unit] = for {
    datasetPemissions <- DataSetStream.findAllPermissions()
  } yield ()

  override def run(args: List[String]): IO[ExitCode] =
    IO(println("Watcher started")) *>
      doRepeatedly(IO(print("Tick..."))) *>
      IO(ExitCode.Success)
}
