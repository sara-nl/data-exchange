package watcher

import cats.effect.{ExitCode, IO, IOApp, Timer}
import cats.implicits._
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto._
import tasker.config.TaskerConfig
import tasker.config.TaskerConfig.queues
import tasker.queue.Messages.StartContainer
import tasker.queue.{AmqpCodecs, Messages}
import tasker.queue.QueueResources.rabbitClientResource

object WatcherApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  private val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    TaskerConfig.watcher.jdbcUrl,
    TaskerConfig.watcher.dbUser,
    TaskerConfig.watcher.dbPassword
  )

  override protected implicit def timer: Timer[IO] =
    TaskerConfig.concurrency.newTimer("watcher-timer")

  private val todoPublisherResource =
    for {
      rabbit <- rabbitClientResource
      publisher <- rabbit.createConnectionChannel.evalMap { implicit channel =>
        for {
          _ <- rabbit.declareQueue(queues.todo.config)
          _ <- rabbit.declareExchange(queues.todo.exchangeConfig)
          _ <- rabbit.bindQueue(
            queues.todo.config.queueName,
            queues.todo.exchangeConfig.exchangeName,
            queues.todo.routingKey
          )
          publisher <- {
            rabbit.createPublisher(
              queues.todo.exchangeConfig.exchangeName,
              queues.todo.routingKey
            )(channel, AmqpCodecs.encoder[StartContainer])
          }
        } yield publisher
      }
    } yield publisher

  override def run(args: List[String]): IO[ExitCode] =
    logger.info(
      s"Watcher started (interval ${TaskerConfig.watcher.awakeInterval})"
    ) *>
      todoPublisherResource.use { publisher =>
        fs2.Stream
          .awakeEvery[IO](TaskerConfig.watcher.awakeInterval)
          .evalTap(_ => logger.debug(s"Fetching eligible permissions from DB"))
          .through(_.flatMap { _ =>
            fs2.Stream
              .evalSeq(Permission.findAllPermissions(xa))
              .evalTap(
                p =>
                  logger.debug(
                    s"Reacting on permission $p \n (already applied for ${p._2.size} datasets"
                )
              )
              .through(DataSet.newDatasetsPipe)
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
      } *>
      IO(ExitCode.Success)
}
