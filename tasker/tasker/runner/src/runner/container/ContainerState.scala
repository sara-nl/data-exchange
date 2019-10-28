package runner.container

object ContainerState {

  case class Exited(code: Int, containerState: String, output: String)
      extends ContainerState

  case object Unknown extends ContainerState

}

sealed trait ContainerState
