package nl.surf.dex.app

import cats.effect.{ExitCode, Fiber, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.concurrency.ConcurrencyResources

object DexThinService {

  private val logger = Slf4jLogger.getLogger[IO]

  def thinService(ioApp: IOApp, name: String) = new IOApp {

    def start(args: List[String]): IO[Fiber[IO, ExitCode]] = {
      logger.info(s"Starting $name") *> ioApp
        .run(args)
        .start(ConcurrencyResources.newFixedContextShift(name))
    }

    override def run(args: List[String]): IO[ExitCode] = ioApp.run(args)
  }

}
