package nl.surf.dex.storage.owncloud

import cats.data.Kleisli
import cats.effect.{ContextShift, IO, Resource}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Json
import io.circe.generic.auto._
import nl.surf.dex.storage.owncloud.config.DexResearchDriveConf
import nl.surf.dex.storage.owncloud.config.DexResearchDriveConf.FileBrowserConf
import nl.surf.dex.storage.{CloudStorage, FilesetOps, Share}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, Headers, Request, Uri}

object OwnCloudShares {

  private val logger = Slf4jLogger.getLogger[IO]

  case class Deps(
      httpClientR: Resource[IO, Client[IO]],
      webdavClientR: Resource[IO, FilesetOps],
      creds: BasicCredentials,
      ocConf: DexResearchDriveConf
  )

  def getShares(implicit ec: ContextShift[IO]): Kleisli[IO, Deps, List[Share]] =
    Kleisli {
      case Deps(httpClientR, webdavClientR, creds, rdConf) =>
        val sharesRequest = Request[IO](
          uri = Uri.unsafeFromString(rdConf.sharesSource.toString),
          headers = Headers.of(Authorization(creds))
        )

        for {
          sharesWithMetadata <- httpClientR.use { client =>
            for {
              _ <- logger.trace(s"Begin shares request")
              shares <- client.expect[Json](sharesRequest).flatMap(extractShares)
              _ <- logger.trace(s"End shares request")
              result <- webdavClientR.use { webdav =>
                def ocShare2DexShare(os: OwncloudShare, childrenNames: List[String] = Nil) =
                  Share(
                    CloudStorage.ResearchDrive,
                    os.path.replaceFirst("^/", ""),
                    OwncloudShare.isAlgorithm(os, childrenNames),
                    OwncloudShare.isFolder(os),
                    os.additional_info_owner.getOrElse(os.uid_owner),
                    FileBrowserConf
                      .resolve(rdConf.fileBrowser, os.file_source.toString)
                      .toString
                  )

                shares.map {
                  case os @ OwncloudShare(_, _, _, path, "file", _) =>
                    ocShare2DexShare(os).pure[IO]
                  case os @ OwncloudShare(_, _, _, path, _, _) =>
                    val base = rdConf.webdavBase
                    for {
                      children <-
                        webdav
                          .asInstanceOf[Webdav]
                          .list(base.change(path))
                      childrenNames =
                        children
                          .map(base.change)
                          .flatMap(_.userPath.toList)
                    } yield ocShare2DexShare(os, childrenNames)
                }.parSequence
              }
              _ <- logger.debug(
                s"Retrieved ${result.length} shares from Research Drive"
              )
            } yield result
          }
        } yield sharesWithMetadata
    }

  private def extractShares(json: Json): IO[List[OwncloudShare]] =
    for {
      _ <- logger.trace(
        s"Extracting OC Shares from: ${json.spaces2}"
      )
      shares <- IO.fromEither(
        json.hcursor
          .downField("ocs")
          .downField("data")
          .as[List[OwncloudShare]]
      )
    } yield shares

}
