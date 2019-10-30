package watcher

import cats.effect.IO

case class DataSet(path: String, isDirectory: Boolean)

object DataSet {

  def findAllIn(path: String): IO[String] = {
    ???
  }

}
