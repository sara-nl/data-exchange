package runner

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.Executors

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, IO, Resource}
import clients.DockerContainer
import runner.config.TaskerConfig
import runner.container.ContainerEnv.Artifact
import runner.container.Ids.ImageId
import runner.container.{ContainerCommand, ContainerEnv, ContainerState}
import org.apache.commons.io.FileUtils

object Resources {

  /**
    * An execution context that is safe to use for blocking operations wrapped into a Resource
    */
  val blockerResource: Resource[IO, Blocker] =
    Resource
      .make(IO(Executors.newCachedThreadPool()))(es => IO(es.shutdown()))
      .map(Blocker.liftExecutorService)

  val tempDirResource: Resource[IO, Path] = Resource.make(
    acquire = IO(Files.createTempDirectory("datex_"))
  )(release = dir => IO(FileUtils.deleteDirectory(dir.toFile)))

  /**
    * Resource of a container environment: temporary directories, where all files can be downloaded.
    * Acquire: temp dir created.
    * Release: temp dir recursively deleted.
    */
  def containerEnv(
    startContainerCmd: Messages.StartContainer
  ): Resource[IO, ContainerEnv] =
    tempDirResource.evalMap { tempHome =>
      import cats.implicits._
      val hostCodePath = Paths.get(tempHome.toString, "code")
      val hostDataPath = Paths.get(tempHome.toString, "data")
      val hostOutPath = Paths.get(tempHome.toString, "out")

      IO(hostCodePath.toFile.mkdirs()) *>
        IO(hostDataPath.toFile.mkdirs()) *>
        IO(hostOutPath.toFile.mkdirs()) *>
        ContainerEnv(
          codeArtifact = Artifact.code(hostCodePath, startContainerCmd.codePath),
          dataArtifact = Artifact.data(hostDataPath, startContainerCmd.dataPath),
          outputArtifact = Artifact.output(hostOutPath)
        ).pure[IO]
    }

  /**
    * Resource of an image, which can be used for creating a container for running the algorithm.
    * Acquire: If necessary - container is created, dependencies installed, image created.
    * Release: If necessary - container and image removed.
    */
  def bakedImageWithDeps(
    containerEnv: ContainerEnv,
    requirementsTxtOption: Option[Path]
  )(implicit F: ConcurrentEffect[IO],
    cs: ContextShift[IO]): Resource[IO, Either[ContainerState, ImageId]] =
    requirementsTxtOption match {
      case Some(requirementsTxtContainerPath) =>
        for {
          containerId <- DockerContainer
            .startedContainer(
              containerEnv,
              ContainerCommand.installDeps(requirementsTxtContainerPath),
              TaskerConfig.docker.image
            )
          containerState <- Resource.liftF(
            DockerContainer.terminalStateIO(containerId)
          )
          result <- containerState match {
            case ContainerState.Exited(0, _, _) =>
              DockerContainer
                .imageFromContainer(containerId)
                .map(Right(_).withLeft[ContainerState])
            case otherState =>
              Resource.pure(Left(otherState).withRight[ImageId])
          }
        } yield result
      case None =>
        Resource.pure[IO, Either[ContainerState, ImageId]](
          Right(TaskerConfig.docker.image)
        )
    }

}
