package runner.utils

import java.nio.file.Path

import cats.effect.{IO, Resource}

import scala.io.Source

object FilesIO {

  def readFileContent(path: Path) =
    Resource
      .fromAutoCloseable(IO(Source.fromFile(path.toString)))
      .use(source => IO(source.mkString))

}
