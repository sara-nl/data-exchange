package watcher

import cats.effect.{ExitCode, IO, IOApp, Timer}
import cats.implicits._
import dev.profunktor.fs2rabbit.model.{AmqpMessage, AmqpProperties}
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto._
import io.circe.syntax._
import tasker.config.TaskerConfig
import tasker.config.TaskerConfig.queues
import tasker.queue.Messages
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
            import tasker.queue.Codecs.messageEncoder
            rabbit.createPublisher[AmqpMessage[String]](
              queues.todo.exchangeConfig.exchangeName,
              queues.todo.routingKey
            )
          }
        } yield publisher
      }
    } yield publisher

  override def run(args: List[String]): IO[ExitCode] =
    logger.info("Watcher started") *>
      todoPublisherResource.use { implicit publisher =>
        fs2.Stream
          .awakeEvery[IO](TaskerConfig.watcher.awakeInterval)
          .evalTap(_ => logger.debug(s"Fetching eligible permissions from DB"))
          .through(_.flatMap { _ =>
            fs2.Stream
              .evalSeq(Permission.findAllPermissions(xa))
              .evalTap(p => logger.debug(s"Reacting on permission $p"))
              .through(DataSet.newDatasetsPipe)
              .evalTap {
                case (newDataset, permission) =>
                  for {
                    _ <- logger
                      .info(s"Processing new dataset ${newDataset.userPath}")
                    taskId <- Task.insert(xa)(
                      permission,
                      newDataset.userPath.getOrElse("/")
                    )
                    _ <- logger.info(s"Created a new task in the DB $taskId")
                    body = Messages.StartContainer(
                      s"$taskId",
                      newDataset.userPath.getOrElse("/"),
                      permission.algorithmPath.userPath.getOrElse("/"),
                      permission.algorithmETag.map(Messages.ETag.apply)
                    )
                    _ <- publisher(
                      AmqpMessage(body.asJson.spaces2, AmqpProperties())
                    )
                  } yield ()
              }
              .evalTap(ds => logger.info(s"Processing a new dataset $ds"))
          })
          .compile
          .drain
      } *>
      IO(ExitCode.Success)
}
