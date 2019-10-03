/**
 * Messages used in the DataExchange messaging protocol
 */
object Messages {

  case class StartContainer(taskId: String, dataPath: String, codePath: String)

  case class Done(taskId: String, state: String, output: String)

}
