package shares

import shares.conf.SharesConf
import shares.conf.SharesConf.ServerConf
import shares.http.HttpClient
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.storage.owncloud.Webdav
import io.github.mkotsur.artc.ActiveReadThroughCache
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

    def server(scs: ActiveReadThroughCache[List[Share]], config: ServerConf) = {
      import nl.surf.dex.storage.CloudStorage.codec._
      val sharesRoutes = HttpRoutes.of[IO] {
        case GET -> Root / "all" =>
          Ok(for {
            _ <- scs.reset().start
            shares <- scs.mostRecent
          } yield shares)
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
      _ <- logger.info("Shares cacher starting")
      config <- SharesConf.loadIO
      rdConf <- DexResearchDriveConf.loadIO
      _ <- logger.info("ARTcy starting")
      gdriveConf <- DexGDriveConf.loadIO
      ocDeps = OwnCloudShares.Deps(
        HttpClient.blazeClientR(config.client),
        Webdav.makeWebdavClient.run(rdConf),
        BasicCredentials(rdConf.webdavUsername, rdConf.webdavPassword),
        rdConf
      )
      sharesCachingService <- ActiveReadThroughCache.create(
        settings = config.update,
        fetchValue = {
          (for {
            shares1 <- GDriveShares.getShares.run(gdriveConf)
            _ <- logger.info(s"Fetched ${shares1.length} gdrive shares")
            shares2 <- OwnCloudShares.getShares.run(ocDeps)
            _ <- logger.info(s"Fetched ${shares2.length} OC shares")
          } yield shares1 ++ shares2).handleErrorWith { e =>
            logger.error(e)("Could not fetch shares") >>
              IO.raiseError(e)
          }
        }
      )
      _ <- logger.info("Shares cacher started")
      serverFiber <- server(sharesCachingService, config.server).start
      updateSharesFiber <- sharesCachingService.scheduleUpdates
      _ <- updateSharesFiber.join
      _ <- serverFiber.join
    } yield ExitCode.Success
  }
}
