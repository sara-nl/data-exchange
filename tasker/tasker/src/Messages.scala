/**
  * Messages used in the DataExchange messaging protocol
  */
object Messages {

  case class StartContainer(taskId: String, dataPath: String, codePath: String)

  // TODO Create enum for state (see backend/surfsara/models/task.py)
  case class Done(taskId: String, state: String, output: String)

}
