package watcher

import cats.effect.IO
import tasker.webdav.WebdavPath

case class Permission(algorithmPath: WebdavPath, dataSetPath: WebdavPath)

object Permission {
  case class AlgorithmRun(dataSetPath: WebdavPath)

  /**
    * Fetches permissions of type streaming from the DB along with happened algorithm runs
    */
  def findAllPermissions(): IO[List[(Permission, List[AlgorithmRun])]] =
    //TODO: implement once the data is available in DB
    IO(
      List(
        (
          Permission(WebdavPath("demo1_code"), WebdavPath("demo1_data")),
          List(AlgorithmRun(WebdavPath("demo1_data/data.json")))
        )
      )
    )

}
