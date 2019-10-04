# DataExchange tasker 🎆

Tasker is a decoupled component, which executes long-running tasks and notifies about the results. It depends on [RabbitMQ](https://www.rabbitmq.com/), which implements [AMQP 0.9.1 protocol](https://www.rabbitmq.com/tutorials/amqp-concepts.html). 

⚠ AMQP is a programmable protocol, which gives application developers a lot of freedom, so it's a good idea to understand basic concepts of AMQP before changing this service.

See also: [Spike Asynchrony](https://www.notion.so/Spike-Asynchrony-71c015cc8e6645689a16f35b59bd45bb).

## Planned functionality:

* Starting / stopping container (0%)
* ... TBD

## Deployment

* Install Java 11
* Install Docker
* Install and start RabbitMQ docker container

## Development

### Generating an Intellij project
```
mill mill.scalalib.GenIdea/idea
```

### Used libraries

* Cats Effect
* [fs2-rabbit](https://fs2-rabbit.profunktor.dev/guide.html)