package container

object Ids {
  case class ContainerId(value: String) extends AnyVal
  case class ImageId(value: String) extends AnyVal
}
