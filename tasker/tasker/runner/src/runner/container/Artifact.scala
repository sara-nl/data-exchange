package runner.container

import java.nio.file.{Path, Paths}

import cats.effect.IO
import cats.implicits._

/**
  * An artifact is a file/folder, which can be accessed on the host (where the JVM process runs),
  * or inside the container (e.g. by executing a command in the container).
  */
sealed trait Artifact

object Artifact {

  case class Location(localHome: Path, containerHome: Path, userPath: String) {
    val localPath: IO[Path] = IO(Paths.get(localHome.toString, userPath))
    val containerPath: IO[Path] = IO(
      Paths.get(containerHome.toString, userPath)
    )
    val isDirectory: IO[Boolean] = localPath.map(_.toFile.isDirectory)
    val exists: IO[Boolean] = localPath.map(_.toFile.exists())

    def child(name: String): IO[Location] =
      IO(copy(userPath = Paths.get(userPath, name).toString))
  }

  case class InputData(location: Location) extends Artifact

  case class OutputData(location: Location) extends Artifact

  case class Algorithm(location: Location, executable: Location, requirements: Option[Location])
      extends Artifact

  def output(location: Location) = OutputData(location).pure[IO]

  def data(location: Location) = InputData(location).pure[IO]

  def algorithm(location: Location): IO[Algorithm] = {

    val depsLocation =
      location.copy(userPath = s"${location.userPath}/requirements.txt")

    (for {
      exists <- location.exists
      isDir <- location.isDirectory
      depsFileOption <- depsLocation.exists.map(e =>
        if (e) Some(depsLocation)
        else None
      )
    } yield (exists, isDir, depsFileOption)).flatMap {
      case (false, _, _) =>
        IO.raiseError(new RuntimeException("Algorithm location doesn't exist"))
      case (_, true, depsFileOption) =>
        Algorithm(
          location,
          location.copy(userPath = s"${location.userPath}/run.py"),
          depsFileOption
        ).pure[IO]
      case (_, false, depsFileOption) =>
        Algorithm(location, location, depsFileOption).pure[IO]
    }
  }

}
