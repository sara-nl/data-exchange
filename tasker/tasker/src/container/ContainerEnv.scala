package container

import java.nio.file.{Path, Paths}

import cats.effect.IO
import com.github.dockerjava.api.model.{AccessMode, Bind, Volume}
import config.TaskerConfig
import config.TaskerConfig.docker
import container.ContainerEnv.{Artifact, Executable, OutputFiles}

object ContainerEnv {

  /**
    * This class models an artifact (folder or file) that can be mounted into a container.
    * @param hostHome Base directory of the artifact on the host
    * @param containerHome Base directory of the artifact on the container
    * @param relativePath Relative path to the code artifact (can be folder, or file)
    */
  case class Artifact(hostHome: Path,
                      containerHome: Path,
                      relativePath: String) {

    /**
      * Full path to the artifact on the host
      */
    val hostPath: Path = Paths.get(hostHome.toString, relativePath)

    /**
      * Full path to the artifact in the container
      */
    val containerPath: Path = Paths.get(containerHome.toString, relativePath)

    /**
      * Whether the artifact is a directory. Must use host path, because the code is executed there. Wrapped into IO,
      * because this is an IO operation and needs to be execute in the right time.
      */
    val isDirectory: IO[Boolean] = IO(hostPath.toFile.isDirectory)

    /**
      * Creates a bind description for given artifact, which can be used for creating a container
      * @param accessMode access mode of the bind
      * @return a Docker bind
      */
    def asBind(accessMode: AccessMode): Bind = {
      new Bind(
        hostHome.toString,
        new Volume(containerHome.toString),
        accessMode
      )
    }
  }

  object Artifact {
    // TODO: make outputPath an option
    def output(hostHome: Path) =
      new Artifact(hostHome, Path.of(docker.containerOutPath), ".")
      with OutputFiles

    def data(hostHome: Path, relativePath: String) =
      new Artifact(hostHome, Path.of(docker.containerDataPath), relativePath)

    def code(hostHome: Path, relativePath: String) =
      new Artifact(hostHome, Path.of(docker.containerCodePath), relativePath)
      with ContainerEnv.Executable
  }

  trait Executable {
    this: Artifact =>
    def executablePath: IO[Path] = isDirectory.map {
      case true =>
        Paths.get(containerPath.toString, TaskerConfig.docker.indexFile)
      case false =>
        containerPath
    }
  }

  trait OutputFiles {
    this: Artifact =>

    val hostStdoutFilePath =
      Paths.get(hostHome.toString, "stdout.txt")

    val containerStdoutFilePath =
      Paths.get(containerHome.toString, "stdout.txt")

    val hostStderrFilePath =
      Paths.get(hostHome.toString, "stderr.txt")

    val containerStderrFilePath =
      Paths.get(containerHome.toString, "stderr.txt")

    val hostStraceFilePath =
      Paths.get(hostHome.toString, "strace.txt")

    val containerStraceFilePath =
      Paths.get(containerHome.toString, "strace.txt")
  }

}

/**
  * Container environment
  * @param codeArtifact contains the algorithm
  * @param dataArtifact contains the data
  * @param outputArtifact contains the data created during execution of the container
  */
case class ContainerEnv(codeArtifact: Artifact with Executable,
                        dataArtifact: Artifact,
                        outputArtifact: Artifact with OutputFiles)
