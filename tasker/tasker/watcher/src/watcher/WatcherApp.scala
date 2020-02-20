package watcher

import cats.effect.{ExitCode, IO, IOApp, Resource, Timer}
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto._
import tasker.concurrency.ConcurrencyResources
import tasker.config.CommonConf
import tasker.config.CommonConf.{QueuesConf, RabbitmqConf}
import tasker.queue.Messages.StartContainer
import tasker.queue.{AmqpCodecs, Messages}
import tasker.queue.QueueResources.rabbitClientResource
import tasker.webdav.WebdavPath
import watcher.WatcherConf.DbConf

import scala.concurrent.duration.FiniteDuration

object WatcherApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  private def xaTransactor(dbConf: DbConf) = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    dbConf.jdbcUrl,
    dbConf.username,
    dbConf.password
  )

  override protected implicit def timer: Timer[IO] =
    ConcurrencyResources.newTimer("watcher-timer")

  private def todoPublisherResource(queuesConf: QueuesConf,
                                    rabbitmqConf: RabbitmqConf) = {
    import tasker.config.conversions._
    for {
      rabbit <- rabbitClientResource(rabbitmqConf)
      publisher <- rabbit.createConnectionChannel.evalMap { implicit channel =>
        for {
          _ <- rabbit.declareQueue(queuesConf.todo.queueConfig)
          _ <- rabbit.declareExchange(queuesConf.todo.exchangeConfig)
          (queueName, exchangeName, routingKey) = queuesConf.todo.asTuple
          _ <- rabbit.bindQueue(queueName, exchangeName, routingKey)
          publisher <- rabbit.createPublisher(exchangeName, routingKey)(
            channel,
            AmqpCodecs.encoder[StartContainer]
          )
        } yield publisher
      }
    } yield publisher
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
            .evalSeq(Permission.findAllPermissions(xa, webdavBase))
            .evalTap(
              p =>
                logger.debug(
                  s"Reacting on permission $p \n (already applied for ${p._2.size} datasets"
              )
            )
            .through(DataSet.newDatasetsPipe(webdavBase))
            .evalTap {
              case (newDataset, eTag, permission) =>
                for {
                  _ <- logger
                    .info(s"Processing new data set ${newDataset.userPath}")
                  taskId <- Task.insert(xa)(
                    permission,
                    newDataset.userPath.getOrElse("/"),
                    eTag
                  )
                  _ <- logger.info(s"Created a new task in the DB $taskId")
                  _ <- publisher(
                    Messages.StartContainer(
                      s"$taskId",
                      newDataset.userPath.getOrElse("/"),
                      permission.algorithmPath.userPath.getOrElse("/"),
                      Messages.ETag(permission.algorithmETag)
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
      config <- WatcherConf.loadF
      commonConfig <- CommonConf.loadF
      _ <- logger.info(s"Watcher started: (interval ${config.awakeInterval})")
      _ <- scheduleWatcher(
        todoPublisherResource(commonConfig.queues, commonConfig.rabbitmq),
        xaTransactor(config.db),
        config.awakeInterval,
        commonConfig.webdavBase
      )
    } yield ExitCode.Success
}
