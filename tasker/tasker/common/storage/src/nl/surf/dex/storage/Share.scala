package nl.surf.dex.storage

import java.io.FileNotFoundException

import cats.data.NonEmptyList
import cats.effect.IO

case object Share {

  object NePath {
    def apply(head: String): NePath = NePath(NonEmptyList(head, Nil))

    def parseIO(so: Option[String]): IO[NePath] =
      so match {
        case None    => IO.raiseError(errors.invalidPathNone)
        case Some(s) => parseIO(s)
      }

    def parseIO(s: String) =
      IO.fromOption(NonEmptyList.fromList(s.split("/").toList))(
          errors.invalidPathString(s)
        )
        .map(NePath.apply)
  }

  /**
    * Non empty path
    */
  case class NePath(segments: NonEmptyList[String])

  case class Location(storage: CloudStorage, path: NePath)

  object Location {
    def parseIO(storage: String, path: String): IO[Location] =
      for {
        s <- IO.fromEither(CloudStorage.codec.parseLabel(storage))
        p <- Share.NePath.parseIO(path)
      } yield Location(s, p)
  }

  object errors {
    def notFound(s: Share.NePath): Throwable =
      new FileNotFoundException(s"Share $s could not be found")

    def invalidPathString(s: String): Throwable =
      new RuntimeException(s"Invalid path $s")

    def invalidPathNone: Throwable =
      new RuntimeException(s"Can not create path from None")
  }

}

case class Share(
    storage: CloudStorage,
    path: String,
    isAlgorithm: Boolean,
    isDirectory: Boolean,
    ownerEmail: String,
    webLink: String
)
