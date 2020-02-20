package runner

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.config.CommonConf
import tasker.queue.Messages.{StartContainer, TaskProgress}
import tasker.queue.{AmqpCodecs, QueueResources}

object RunnerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  private def handleMessages(commonConf: CommonConf, runnerConf: RunnerConf) =
    QueueResources.rabbitClientResource(commonConf.rabbitmq).use { rabbit =>
      rabbit.createConnectionChannel.use { implicit channel =>
        import tasker.config.conversions._
        import tasker.queue.Messages.implicits._
        for {
          _ <- rabbit.declareQueue(commonConf.queues.todo.queueConfig)
          _ <- rabbit.declareExchange(commonConf.queues.todo.exchangeConfig)
          consumer <- {
            val (queueName, exchangeName, routingKey) =
              commonConf.queues.todo.asTuple
            rabbit.bindQueue(queueName, exchangeName, routingKey) *>
              rabbit.createAutoAckConsumer(queueName)(
                channel,
                AmqpCodecs.decoder[StartContainer]
              )
          }
          _ <- rabbit.declareQueue(commonConf.queues.done.queueConfig)
          _ <- rabbit.declareExchange(commonConf.queues.done.exchangeConfig)
          publisher <- {
            val (queueName, exchangeName, routingKey) =
              commonConf.queues.done.asTuple
            rabbit.bindQueue(queueName, exchangeName, routingKey) *>
              rabbit.createPublisher(exchangeName, routingKey)(
                channel,
                AmqpCodecs.encoder[TaskProgress]
              )
          }
          _ <- new SecureContainerFlow(
            consumer,
            publisher,
            commonConf.webdavBase,
            runnerConf.docker
          ).flow.compile.drain
        } yield ()
      }
    }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- logger.info("Runner started")
      commonConf <- CommonConf.loadF
      conf <- RunnerConf.loadF
      _ <- handleMessages(commonConf, conf)
    } yield ExitCode.Success
}
