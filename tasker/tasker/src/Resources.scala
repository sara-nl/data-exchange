import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.Executors

import cats.effect.{Blocker, IO, Resource}
import container.ContainerEnv
import container.ContainerEnv.Artifact
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

  def containerEnvironmentResource(
    startContainerCmd: Messages.StartContainer
  ): Resource[IO, ContainerEnv] =
    tempDirResource.evalMap { tempHome =>
      import cats.implicits._
      import cats.syntax._
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

}
