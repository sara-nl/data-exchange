package runner.container

import java.nio.file.Path

object ContainerCommand {
  def runWithStrace(env: ContainerEnv) =
    for {
      algorithmContainerPath <- env.algorithm.executable.containerPath
      inputContainerPath <- env.input.location.containerPath
      stdoutContainerPath <- env.stdout.flatMap(_.containerPath)
      stderrContainerPath <- env.stderr.flatMap(_.containerPath)
      straceContainerPath <- env.strace.flatMap(_.containerPath)
    } yield
      ContainerCommand(
        "/app/tracerun.sh",
        List(
          algorithmContainerPath.toString,
          inputContainerPath.toString,
          stdoutContainerPath.toString,
          stderrContainerPath.toString,
          straceContainerPath.toString
        )
      )

  def installDeps(requirementsContainerPath: Path) =
    ContainerCommand(
      "pip",
      List("install", "-r", requirementsContainerPath.toString),
      secureContainer = false
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
