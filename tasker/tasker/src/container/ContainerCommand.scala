package container

import java.nio.file.Path

import cats.effect.IO

object ContainerCommand {
  def runWithStrace(env: ContainerEnv) =
    for { codeEntryPoint <- env.codeArtifact.executablePath } yield
      ContainerCommand(
        "/app/tracerun.sh",
        List(
          codeEntryPoint.toString,
          env.dataArtifact.containerPath.toString,
          env.outputArtifact.containerStdoutFilePath.toString,
          env.outputArtifact.containerStderrFilePath.toString,
          env.outputArtifact.containerStraceFilePath.toString
        )
      )

  def installDeps(requirementsContainerPath: Path) =
    IO.pure(
      ContainerCommand(
        "pip",
        List("install", "-r", requirementsContainerPath.toString),
        secureContainer = false
      )
    )
}

/**
  * Represents a command that can be executed in a container.
  */
case class ContainerCommand(executable: String,
                            arguments: List[String],
                            secureContainer: Boolean = true) {
  def toArgs = executable :: arguments
}
