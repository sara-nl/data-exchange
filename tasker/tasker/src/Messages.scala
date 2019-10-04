/**
 * Messages used in the DataExchange messaging protocol
 */
object Messages {

  case class StartContainer(taskId: Int, dataPath: String, codePath: String)

  // TODO Create enum for state (see backend/surfsara/models/task.py)
  case class Done(taskId: Int, state: String, output: String)

}
