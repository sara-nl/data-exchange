package cacher.service

import cacher.conf.CacherConf
import cacher.conf.CacherConf.ClientConf
import cacher.model.Share
import cacher.model.Share.ShareMetadata
import cats.effect.{ContextShift, IO}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.headers.Authorization
import org.http4s.{Request, _}
import tasker.config.CommonConf
import tasker.webdav.{Webdav, WebdavPath}

import scala.concurrent.ExecutionContext.global

object SharesService {

  private val logger = Slf4jLogger.getLogger[IO]

  private def withMetadata(
    webdavBase: WebdavPath
  )(share: Share): IO[ShareMetadata] = share match {
    case Share(_, _, path, "file", _) =>
      ShareMetadata(path.endsWith(".py"), share).pure[IO]
    case Share(_, _, path, _, _) =>
      for {
        webdav <- Webdav.makeClient
        resources <- webdav.list(webdavBase.change(path))
      } yield
        ShareMetadata(resources.exists(_.getPath.endsWith("run.py")), share)
  }

  def getShares(implicit ec: ContextShift[IO]): IO[List[ShareMetadata]] =
    for {
      cacherConf <- CacherConf.loadF
      commonConf <- CommonConf.loadF
      sharesWithMetadata <- prepareBlazeClient(cacherConf.client).use {
        client =>
          for {
            json <- client.expect[Json](
              Request[IO](
                uri = Uri.unsafeFromString(cacherConf.sharesSource.toString),
                headers = Headers.of(
                  Authorization(
                    BasicCredentials(
                      commonConf.researchDrive.webdavUsername,
                      commonConf.researchDrive.webdavPassword
                    )
                  )
                )
              )
            )
            _ <- logger.trace(s"Research Drive returned JSON: ${json.spaces2}")
            shares <- IO.fromEither(
              json.hcursor
                .downField("ocs")
                .downField("data")
                .as[List[Share]]
            )
            result <- shares
              .map(withMetadata(commonConf.webdavBase))
              .parSequence
            _ <- logger.debug(
              s"Retrieved ${result.length} shares from Research Drive"
            )
          } yield result
      }
    } yield sharesWithMetadata

  private def prepareBlazeClient(
    conf: ClientConf
  )(implicit ec: ContextShift[IO]) =
    BlazeClientBuilder[IO](global)
      .withConnectTimeout(conf.connectionTimeout)
      .withResponseHeaderTimeout(conf.responseHeaderTimeout)
      .withRequestTimeout(conf.requestTimeout)
      .withIdleTimeout(conf.idleTimeout)
      .resource
}
