package runner

import java.nio.file.{Files, Path, Paths}

import cats.effect.{Concurrent, ConcurrentEffect, IO, Resource}
import org.apache.commons.io.FileUtils
import runner.RunnerConf.DockerConf
import runner.container.Artifact.Location
import runner.container.docker.Ids.ImageId
import runner.container.docker.{DockerOps, Ids}
import runner.container.{
  Artifact,
  ContainerCommand,
  ContainerEnv,
  ContainerState
}
import tasker.queue.Messages
import tasker.webdav.{Webdav, WebdavPath}

object Resources {

  val tempDirResource: Resource[IO, Path] = Resource.make(
    acquire = IO(Files.createTempDirectory("datex_"))
  )(release = dir => IO(FileUtils.deleteDirectory(dir.toFile)))

  /**
    * Resource of a container environment: temporary directories, where all files can be downloaded.
    * Acquire: temp dir created.
    * Release: temp dir recursively deleted.
    */
  def containerEnv(
    startContainerCmd: Messages.StartContainer,
    dockerConf: DockerConf,
    webdavBase: WebdavPath
  )(implicit F: Concurrent[IO]): Resource[IO, ContainerEnv] =
    tempDirResource.evalMap { tempHome =>
      import cats.implicits._

      // TODO: move to `common` WebdavPath apply method
      def webdavPath(location: Location) = webdavBase.change(location.userPath)

      val algorithmLocation = Location(
        Paths.get(tempHome.toString, "code"),
        Path.of(dockerConf.containerCodePath),
        startContainerCmd.codePath
      )

      val inputLocation = Location(
        Paths.get(tempHome.toString, "data"),
        Path.of(dockerConf.containerDataPath),
        startContainerCmd.dataPath
      )

      val outputLocation =
        Location(
          Paths.get(tempHome.toString, "out"),
          Path.of(dockerConf.containerOutPath),
          "."
        )

      val downloads = Map(
        webdavPath(algorithmLocation) -> algorithmLocation.localHome,
        webdavPath(inputLocation) -> inputLocation.localHome
      )

      val newDirs = List(algorithmLocation, inputLocation, outputLocation)

      for {
        webdav <- Webdav.makeClient
        _ <- newDirs.map(l => IO(l.localHome.toFile.mkdirs())).sequence
        _ <- webdav.downloadToHost(downloads)
        algorithm <- Artifact.algorithm(algorithmLocation)
        input <- Artifact.data(inputLocation)
        output <- Artifact.output(outputLocation)
      } yield ContainerEnv(algorithm, input, output)

    }

  /**
    * Resource of an image, which can be used for creating a container for running the algorithm.
    * Acquire: If necessary - container is created, dependencies installed, image created.
    * Release: If necessary - container and image removed.
    */
  def bakedImageWithDeps(containerEnv: ContainerEnv, dockerConf: DockerConf)(
    implicit F: ConcurrentEffect[IO]
  ): Resource[IO, Either[ContainerState, ImageId]] =
    containerEnv.algorithm.requirements match {
      case Some(requirementsLocation) =>
        for {
          reqContainerPath <- Resource.liftF(requirementsLocation.containerPath)
          containerId <- DockerOps
            .startedContainer(
              containerEnv,
              ContainerCommand.installDeps(reqContainerPath),
              Ids.ImageId(dockerConf.image)
            )
          containerState <- Resource.liftF(
            DockerOps.terminalStateIO(containerId)
          )
          result <- containerState match {
            case ContainerState.Exited(0, _, _) =>
              DockerOps
                .imageFromContainer(containerId)
                .map(Right(_).withLeft[ContainerState])
            case otherState =>
              Resource.pure(Left(otherState).withRight[ImageId])
          }
        } yield result
      case None =>
        Resource.pure[IO, Either[ContainerState, ImageId]](
          Right(Ids.ImageId(dockerConf.image))
        )
    }

}
