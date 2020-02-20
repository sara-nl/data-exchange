package cacher

import cacher.conf.CacherConf
import cacher.conf.CacherConf.ServerConf
import cacher.service.SharesCachingService
import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityEncoder._

object CacherApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {

    def server(scs: SharesCachingService, config: ServerConf) = {
      val sharesRoutes = HttpRoutes.of[IO] {
        case GET -> Root / "all" =>
          Ok(for {
            _ <- scs.reset().start
            shares <- scs.sharesRef.get
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
      config <- CacherConf.loadF
      sharesCachingService <- SharesCachingService.create(config)
      _ <- logger.info("Cacher started")
      serverFiber <- server(sharesCachingService, config.server).start
      updateSharesFiber <- sharesCachingService.scheduleUpdates.start
      _ <- updateSharesFiber.join
      _ <- serverFiber.join
    } yield ExitCode.Success
  }
}
