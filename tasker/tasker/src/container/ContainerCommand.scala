package container

import cats.effect.IO

object ContainerCommand {
  def runWithStrace(env: ContainerEnv) =
    for { codeEntryPoint <- env.codeArtifact.executablePath } yield
      ContainerCommand(
        "/app/tracerun.sh",
        List(
          codeEntryPoint,
          env.dataArtifact.containerPath.toString,
          env.outputArtifact.containerStdoutFilePath.toString,
          env.outputArtifact.containerStderrFilePath.toString,
          env.outputArtifact.containerStraceFilePath.toString
        )
      )

  def installDeps(env: ContainerEnv) =
    IO.pure(ContainerCommand(???, List(???), secureContainer = false))
}

/**
  * Represents a command that can be executed in a container.
  */
case class ContainerCommand(executable: String,
                            arguments: List[String],
                            secureContainer: Boolean = true) {
  def toArgs = executable :: arguments
}
