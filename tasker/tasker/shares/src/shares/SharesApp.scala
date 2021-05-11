package shares

import shares.conf.SharesConf
import shares.conf.SharesConf.ServerConf
import shares.http.HttpClient
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.storage.owncloud.Webdav
import io.github.mkotsur.artc.Cache
import nl.surf.dex.storage.Share
import nl.surf.dex.storage.gdrive.GDriveShares
import nl.surf.dex.storage.gdrive.config.DexGDriveConf
import nl.surf.dex.storage.owncloud.OwnCloudShares
import nl.surf.dex.storage.owncloud.config.DexResearchDriveConf
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

object SharesApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    def server(scs: Cache[List[Share]], config: ServerConf) = {
      import nl.surf.dex.storage.CloudStorage.codec._
      val sharesRoutes = HttpRoutes.of[IO] {
        case GET -> Root / "all" =>
          Ok(scs.latest)
      }

      BlazeServerBuilder[IO]
        .bindHttp(8088, "0.0.0.0")
        .withIdleTimeout(config.idleTimeout)
        .withResponseHeaderTimeout(config.responseHeaderTimeout)
        .withHttpApp(Router("/shares" -> sharesRoutes).orNotFound)
        .resource
        .use(_ => IO.never)
    }

    for {
      _ <- logger.info("Shares service starting")
      config <- SharesConf.loadIO
      artcSettings = {
        Cache.Settings(config.update.ceilingInterval, config.update.initialInterval)
      }
      rdConf <- DexResearchDriveConf.loadIO
      gdriveConf <- DexGDriveConf.loadIO
      ocDeps = OwnCloudShares.Deps(
        HttpClient.blazeClientR(config.client),
        Webdav.makeWebdavClient.run(rdConf),
        BasicCredentials(rdConf.webdavUsername, rdConf.webdavPassword),
        rdConf
      )
      _ <- {
        Cache
          .create(
            artcSettings, {
              (for {
                shares1 <- GDriveShares.getShares.run(gdriveConf)
                _ <- logger.info(s"Fetched ${shares1.length} gdrive shares")
                shares2 <- OwnCloudShares.getShares.run(ocDeps).handleErrorWith { e =>
                  logger.error(e)("Could not fetch OC shares") >>
                    IO.pure(Nil)
                }
                _ <- logger.trace("Finished fetch of OC shares")
                _ <- logger.info(s"Fetched ${shares2.length} OC shares")
              } yield shares1 ++ shares2).handleErrorWith { e =>
                logger.error(e)("Could not fetch shares") >>
                  IO.raiseError(e)
              }
            }
          )
          .use(cache => server(cache, config.server).start.flatMap(_.join))
      }
    } yield ExitCode.Success
  }
}
