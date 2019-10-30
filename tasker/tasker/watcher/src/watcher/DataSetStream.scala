package watcher

import cats.effect.IO

object DataSetStream {

  case class Permission(id: String, algorithmPath: String, dataSetPath: String)
  case class AlgorithmRun()

  /**
    *
    */
  def findAllPermissions(): IO[(Permission, List[AlgorithmRun])] =
    ???

}
