package runner.container

object ContainerState {

  case class Exited(
      /**
        * Exit code.
        */
      code: Int,
      /**
        * Docker state of the container.
        */
      containerState: String,
      /**
        * This method is not used for now, because we output into files using a wrapper script. It will, however,
        * be useful when we want to send intermediate outputs along with task states.
        */
      output: String
  ) extends ContainerState

  case object Unknown extends ContainerState

}

sealed trait ContainerState
