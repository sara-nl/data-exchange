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
        for {
          sharesWithMetadata <- httpClientR.use { client =>
            for {
              json <- client.expect[Json](
                Request[IO](
                  uri = Uri.unsafeFromString(rdConf.sharesSource.toString),
                  headers = Headers.of(Authorization(creds))
                )
              )
              _ <- logger.trace(
                s"Research Drive returned JSON: ${json.spaces2}"
              )
              shares <- IO.fromEither(
                json.hcursor
                  .downField("ocs")
                  .downField("data")
                  .as[List[OwncloudShare]]
              )
              result <- webdavClientR.use { webdav =>
                shares.map {
                  case os @ OwncloudShare(_, uid_owner, path, "file", _) =>
                    Share(
                      CloudStorage.ResearchDrive,
                      path.replaceFirst("^/", ""),
                      OwncloudShare.isAlgorithm(os),
                      OwncloudShare.isFolder(os),
                      uid_owner,
                      FileBrowserConf
                        .resolve(rdConf.fileBrowser, os.file_source.toString)
                        .toString
                    ).pure[IO]
                  case os @ OwncloudShare(_, uid_owner, path, _, _) =>
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
                    } yield {
                      Share(
                        CloudStorage.ResearchDrive,
                        path.replaceFirst("^/", ""),
                        OwncloudShare.isAlgorithm(os, childrenNames),
                        OwncloudShare.isFolder(os),
                        uid_owner,
                        FileBrowserConf
                          .resolve(rdConf.fileBrowser, os.file_source.toString)
                          .toString
                      )
                    }
                }.parSequence
              }
              _ <- logger.debug(
                s"Retrieved ${result.length} shares from Research Drive"
              )
            } yield result
          }
        } yield sharesWithMetadata
    }

}
