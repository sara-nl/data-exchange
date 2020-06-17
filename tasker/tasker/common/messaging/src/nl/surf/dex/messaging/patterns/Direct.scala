package nl.surf.dex.messaging.patterns

import cats.effect.IO
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.AMQPChannel
import io.circe.{Decoder, Encoder}
import nl.surf.dex.messaging.AmqpCodecs
import nl.surf.dex.messaging.config.QueueConf

object Direct {

  def declareAndBind(conf: QueueConf)(implicit rabbit: RabbitClient[IO],
                                      channel: AMQPChannel): IO[Unit] = {
    val (queueName, exchangeName, routingKey) = conf.asTuple
    rabbit.declareQueue(conf.queueConfig) *>
      rabbit.declareExchange(conf.exchangeConfig) *>
      rabbit.bindQueue(queueName, exchangeName, routingKey)
  }

  def consumer[M: Decoder](conf: QueueConf)(implicit rabbit: RabbitClient[IO],
                                            channel: AMQPChannel) = {
    val (queueName, exchangeName, routingKey) = conf.asTuple
    rabbit.bindQueue(queueName, exchangeName, routingKey) *>
      rabbit
        .createAutoAckConsumer(queueName)(channel, AmqpCodecs.decoder[M])
        .map { consumer =>
          consumer
            .map(_.payload)
            .through(AmqpCodecs.filterAndLogErrors)
        }

  }

  def publisher[M: Encoder](conf: QueueConf)(implicit rabbit: RabbitClient[IO],
                                             channel: AMQPChannel) = {
    val (_, exchangeName, routingKey) = conf.asTuple

    rabbit.createPublisher(exchangeName, routingKey)(
      channel,
      AmqpCodecs.encoder[M]
    )

  }

}
