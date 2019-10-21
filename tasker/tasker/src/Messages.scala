/**
  * Messages used in the DataExchange messaging protocol
  */
object Messages {

  case class StartContainer(taskId: String, dataPath: String, codePath: String)

  object Done {
    def success(taskId: String,
                output: String,
                containerOutput: ContainerOutput) =
      Done(taskId, "success", output, containerOutput)
    def error(taskId: String,
              output: String,
              containerOutput: ContainerOutput) =
      Done(taskId, "error", output, containerOutput)
  }

  case class ContainerOutput(
                             /**
                               * Standard output produced by the user Python script
                               */
                             stdout: String,
                             /**
                               * Standard error produced by the user Python script
                               */
                             stderr: String,
                             /**
                               * Strace produced by the user Python script
                               */
                             strace: String)

  case class Done(
                  /**
                    * The task id from [[StartContainer]] command
                    */
                  taskId: String,
                  /**
                    * "success" | "error"
                    */
                  state: String,
                  /**
                    * Output produced by the wrapper script (doesn't include the output of the user script).
                    */
                  output: String,
                  /**
                    * An object, which contains different types of container output
                    */
                  containerOutput: ContainerOutput)

}
