package runner.container.docker

object Ids {
  case class ContainerId(value: String) extends AnyVal
  case class ImageId(value: String, createdFrom: Option[ContainerId] = None)
}
