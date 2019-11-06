package tasker.algorithm

sealed trait AlgorithmHash {
  val hash: String
}

object AlgorithmHash {
  case class ETag(hash: String) extends AlgorithmHash
}
