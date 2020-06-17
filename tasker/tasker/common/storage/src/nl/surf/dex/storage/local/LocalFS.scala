package nl.surf.dex.storage.local

import better.files.{File => BFile}
import cats.effect.{IO, Resource}

/**
  * Shortcuts for allocating resources of the local filesystem on the server
  */
object LocalFS {

  private val defaultLabel = "datex_"

  /**
    * Creates a resource of a temp directory represented as a BFile
    */
  def tempDir(label: Option[String]): Resource[IO, BFile] =
    Resource.make(
      acquire = IO(BFile.newTemporaryDirectory(label.getOrElse(defaultLabel)))
    )(release = t => IO(t.delete()))

}
