/**
  * Messages used in the DataExchange messaging protocol
  */
object Messages {

  case class StartContainer(taskId: String, dataPath: String, codePath: String)

  object Done {
    def success(taskId: String, output: String) = Done(taskId, "success", output)
    def error(taskId: String, output: String) = Done(taskId, "error", output)
  }

  case class Done(taskId: String, state: String, output: String)

}
