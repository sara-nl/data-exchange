package shares

import shares.conf.SharesConf
import shares.conf.SharesConf.ServerConf
import shares.http.HttpClient
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.storage.owncloud.Webdav
import io.github.mkotsur.artc.{Cache, ReadSourceFailure}
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

  private def httpServerR(shares: IO[List[Share]], config: ServerConf) = {
    import nl.surf.dex.storage.CloudStorage.codec._
    BlazeServerBuilder[IO]
      .bindHttp(config.httpPort, "0.0.0.0")
      .withIdleTimeout(config.idleTimeout)
      .withResponseHeaderTimeout(config.responseHeaderTimeout)
      .withHttpApp(Router("/shares" -> HttpRoutes.of[IO] {
        case GET -> Root / "all" => Ok(shares)
      }).orNotFound)
      .resource
  }

  private def handleFetchError(e: Throwable): IO[None.type] =
    e match {
      case e: ReadSourceFailure =>
        logger.warn(e)("Error when fetching data") >> None.pure[IO]
      case e => IO.raiseError(e)
    }

  override def run(args: List[String]): IO[ExitCode] = {

    logger.info("Shares service starting") >> (for {
      (config, rdConf, gdriveConf) <- Resource.liftF(
        (SharesConf.loadIO, DexResearchDriveConf.loadIO, DexGDriveConf.loadIO).parTupled
      )
      ocDeps = OwnCloudShares.Deps(
        HttpClient.blazeClientR(config.client),
        Webdav.makeWebdavClient.run(rdConf),
        BasicCredentials(rdConf.webdavUsername, rdConf.webdavPassword),
        rdConf
      )
      cacheSettings = {
        import config.update._
        Cache.Settings(ceilingInterval, initialInterval)
      }
      c1 <- Cache.create(cacheSettings, GDriveShares.getShares.run(gdriveConf))
      c2 <- Cache.create(cacheSettings, OwnCloudShares.getShares.run(ocDeps))
      _ <- httpServerR(
        for {
          shares1 <-
            c1.latest
              .handleErrorWith(handleFetchError)
              .map(_.getOrElse(Nil))
          shares2 <-
            c2.latest
              .handleErrorWith(handleFetchError)
              .map(_.getOrElse(Nil))
          _ <- logger.info(s"Fetched ${shares1.size} GD and ${shares2.size} shares")
        } yield shares1 ++ shares2,
        config.server
      )
    } yield ()).use(_ => IO.never).start.flatMap(_.join)

  }
}
