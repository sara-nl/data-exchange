import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.Executors

import cats.effect.{Blocker, IO, Resource}
import clients.DockerContainer
import config.TaskerConfig
import container.{ContainerCommand, ContainerEnv}
import container.ContainerEnv.Artifact
import container.Ids.ImageId
import org.apache.commons.io.FileUtils
import cats.implicits._

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
  def bakedImageResource(
    containerEnv: ContainerEnv,
    requirementsTxtOption: Option[Path]
  ): Resource[IO, ImageId] = requirementsTxtOption match {
    case Some(requirementsTxtContainerPath) =>
      DockerContainer
        .startedContainer(
          containerEnv,
          ContainerCommand.installDeps(requirementsTxtContainerPath),
          TaskerConfig.docker.image
        )
        .evalMap(
          containerId =>
            DockerContainer.lastStatusIO(containerId) *> IO.pure(containerId)
        )
        .flatMap(containerId => DockerContainer.imageFromContainer(containerId))
    case None => Resource.pure(TaskerConfig.docker.image)
  }

}
