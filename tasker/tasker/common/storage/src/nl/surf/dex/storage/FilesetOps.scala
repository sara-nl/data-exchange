package nl.surf.dex.storage

import java.io.FileNotFoundException

import better.files.{File => BFile}
import cats.effect.{ContextShift, IO}
import nl.surf.dex.storage.Share.Location

object FilesetOps {
  object errors {

    case class CanNotListException(location: Location)
        extends RuntimeException(s"$location can not be listed")

    def notFound(l: Location) =
      s"Could not find $l"

    def notFoundException(l: Location) = new FileNotFoundException(notFound(l))
  }
}

trait FilesetOps {

  def getFilesetHash(sharePath: Share.NePath): IO[Fileset.Hash]

  /**
    * Copies files from remote path into a local parent.
    * Returns the local representation of `sharePath`.
    *
    * E.g.: Share.Path(photos) => Local directory "photos" is returned;
    *
    */
  def copySharedFileset(sharePath: Share.NePath, localParent: BFile)(implicit
      cs: ContextShift[IO]
  ): IO[BFile]

  def listShareFolder(sharePath: Share.NePath): IO[List[Share.NePath]]

}
