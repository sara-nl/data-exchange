package nl.surf.dex.storage

import io.circe.generic._
import io.circe.{Decoder, Encoder}

sealed trait CloudStorage extends Product with Serializable {
  def id: String = productPrefix.toLowerCase
}

object CloudStorage {
  case object ResearchDrive extends CloudStorage
  case object GoogleDrive extends CloudStorage

  // See: https://stackoverflow.com/questions/59086167/how-to-convert-sealed-trait-case-objects-to-string-using-circe
  // This codec is a bit weird and probably should be replaced with the approach used in circe-generic-extra's. Or Magnolia fwiw.
  object codec extends AutoDerivation {

    implicit val storageEncode: Encoder[CloudStorage] =
      Encoder[String].contramap { _.id }

    implicit val storageDecode: Decoder[CloudStorage] = Decoder[String].emap {
      label =>
        parseLabel(label).left.map(_.getMessage)
    }

    def parseLabel(s: String): Either[Throwable, CloudStorage] =
      s match {
        case "researchdrive" => Right(CloudStorage.ResearchDrive)
        case "googledrive"   => Right(CloudStorage.GoogleDrive)
        case other           => Left(new RuntimeException(s"Invalid storage $other"))
      }
  }
}
