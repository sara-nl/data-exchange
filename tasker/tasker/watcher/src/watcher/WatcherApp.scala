package watcher

import cats.effect.{ExitCode, IO, IOApp, Resource, Timer}
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto._
import nl.surf.dex.database.Tasks
import nl.surf.dex.database.config.{DbConf, DexTransactor}
import nl.surf.dex.messaging.Messages
import nl.surf.dex.messaging.QueueResources.rabbitClientResource
import nl.surf.dex.messaging.config.DexMessagingConf
import nl.surf.dex.messaging.patterns.Direct
import nl.surf.dex.storage.config.DexStorageConf
import nl.surf.dex.storage.owncloud.WebdavPath
import tasker.concurrency.ConcurrencyResources
import Messages.StartContainer

import scala.concurrent.duration.FiniteDuration

object WatcherApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override protected implicit def timer: Timer[IO] =
    ConcurrencyResources.newTimer("watcher-timer")

  private def todoPublisherResource(conf: DexMessagingConf) = {
    rabbitClientResource(conf.broker).flatMap { implicit rabbit =>
      rabbit.createConnectionChannel.evalMap { implicit channel =>
        Direct.declareAndBind(conf.todo) *>
          Direct.publisher[StartContainer](conf.todo)
      }
    }
  }

  private def scheduleWatcher(
    publisherResource: Resource[IO, StartContainer => IO[Unit]],
    xa: Transactor[IO],
    awakeInterval: FiniteDuration,
    webdavBase: WebdavPath
  ): IO[Unit] = {
    publisherResource.use { publisher =>
      fs2.Stream
        .awakeEvery[IO](awakeInterval)
        .evalTap(_ => logger.debug(s"Fetching eligible permissions from DB"))
        .through(_.flatMap { _ =>
          fs2.Stream
            .evalSeq(Permissions.findAllPermissions(xa))
            .evalTap(
              p =>
                logger.debug(
                  s"Reacting on permission $p \n (already applied for ${p._2.size} datasets"
              )
            )
            .through(DataSet.newDatasetsPipe(webdavBase))
            .evalTap {
              case (newDataset, _, permission) =>
                for {
                  _ <- logger
                    .info(s"Processing new data set ${newDataset.userPath}")
                  taskId <- Tasks
                    .insert(xa)(permission, newDataset.userPath.getOrElse("/"))
                  _ <- logger.info(s"Created a new task in the DB $taskId")
                  _ <- publisher(
                    Messages.StartContainer(
                      s"$taskId",
                      newDataset.userPath.getOrElse("/"),
                      webdavBase
                        .change(permission.algorithmPath)
                        .userPath
                        .getOrElse("/"),
                      permission.algorithmETag
                    )
                  )
                } yield ()
            }
            .evalTap(ds => logger.info(s"Processing a new data set $ds"))
        })
        .compile
        .drain
    }
  }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- WatcherConf.loadIO
      storageConf <- DexStorageConf.loadF
      messagingConf <- DexMessagingConf.loadIO
      dbConf <- DbConf.loadIO
      _ <- logger.info(s"Watcher started: (interval ${config.awakeInterval})")
      _ <- scheduleWatcher(
        todoPublisherResource(messagingConf),
        DexTransactor.create(dbConf),
        config.awakeInterval,
        storageConf.webdavBase
      )
    } yield ExitCode.Success
}
