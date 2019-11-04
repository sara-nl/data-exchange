package watcher

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import dev.profunktor.fs2rabbit.interpreter.Fs2Rabbit
import dev.profunktor.fs2rabbit.model.{AmqpMessage, AmqpProperties}
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.concurrency.ConcurrencyResources
import tasker.config.TaskerConfig
import tasker.config.TaskerConfig.queues
import tasker.queue.Messages
import io.circe.generic.auto._
import io.circe.syntax._

object WatcherApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  // TODO: extract into config
  private val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5433/surfsara",
    "surfsara",
    ""
  )

  // TODO: move into tasker.concurrency
  private val publisherResource = for {
    rabbit <- ConcurrencyResources.blocker.evalMap { blocker =>
      Fs2Rabbit[IO](TaskerConfig.rabbitConfig, blocker)
    }
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
      publisherResource.use { implicit publisher =>
        fs2.Stream
          .awakeEvery[IO](TaskerConfig.watcher.awakeInterval)
          .through(_.flatMap { _ =>
            fs2.Stream
              .evalSeq(Permission.findAllPermissions(xa))
              .evalTap(p => logger.debug(s"Reacting on permission $p"))
              .through(DataSet.newDatasetsPipe)
              .evalTap {
                case (newDataset, permission) =>
                  for {
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
