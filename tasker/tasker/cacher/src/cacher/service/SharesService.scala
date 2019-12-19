package cacher.service

import cacher.conf.CacherConfig
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
import org.http4s.syntax.literals._
import org.http4s.{Request, _}
import tasker.config.TaskerConfig
import tasker.webdav.Webdav

import scala.concurrent.ExecutionContext.global

object SharesService {

  private val logger = Slf4jLogger.getLogger[IO]

  val uri =
    uri"https://researchdrive.surfsara.nl/ocs/v1.php/apps/files_sharing/api/v1/shares?format=json&shared_with_me=true"

  val creds = BasicCredentials("f_data_exchange", "KCVNI-VBXWR-NLGMO-POQNO")

  private def withMetadata(share: Share): IO[ShareMetadata] = share match {
    case Share(_, _, path, "file", _) =>
      ShareMetadata(path.endsWith(".py"), share).pure[IO]
    case Share(_, _, path, _, _) =>
      for {
        resources <- Webdav.list(TaskerConfig.webdav.serverPath.change(path))
      } yield
        ShareMetadata(resources.exists(_.getPath.endsWith("run.py")), share)
  }

  def getShares(implicit ec: ContextShift[IO]): IO[List[ShareMetadata]] = {

    BlazeClientBuilder[IO](global)
      .withConnectTimeout(CacherConfig.server.timeouts.connection)
      .withResponseHeaderTimeout(CacherConfig.server.timeouts.responseHeader)
      .withRequestTimeout(CacherConfig.server.timeouts.request)
      .withIdleTimeout(CacherConfig.server.timeouts.idle)
      .resource
      .use { client =>
        for {
          json <- client.expect[Json](
            Request[IO](uri = uri, headers = Headers.of(Authorization(creds)))
          )
          _ <- logger.trace(s"Research Drive returned JSON: ${json.spaces2}")
          shares <- IO.fromEither(
            json.hcursor
              .downField("ocs")
              .downField("data")
              .as[List[Share]]
          )
          sharesWithMetadata <- shares.map(withMetadata).parSequence
          _ <- logger.debug(
            s"Retrieved ${sharesWithMetadata.length} from Research Drive"
          )
        } yield sharesWithMetadata
      }
  }
}
