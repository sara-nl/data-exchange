# DataExchange Tasker 🎆

Tasker is a decoupled component, which executes long-running tasks and notifies about the results. It depends on [RabbitMQ](https://www.rabbitmq.com/), which implements [AMQP 0.9.1 protocol](https://www.rabbitmq.com/tutorials/amqp-concepts.html).

⚠ AMQP is a programmable protocol, which gives application developers a lot of freedom, so it's a good idea to understand basic concepts of AMQP before changing this service.

See also: [Spike Asynchrony](https://www.notion.so/Spike-Asynchrony-71c015cc8e6645689a16f35b59bd45bb).

## Messaging protocol v0.1

Also see in `tasker/src/Messages.scala`

### Start container (`tasker_todo -> [T]`)

Example:
```json
  {
    "taskId": "123", 
    "codePath": "demo2_code",
    "dataPath": "demo1_data",
    "codeHash": { "eTag": "5db6a51baa9e0" }
  }
```

### Done (`[T] -> tasker_done`)


```json
{
  "taskId" : "123",
  "state" : "success",
  "output" : "",
  "containerOutput" : {
    "stdout" : "Average fortune: 100350000000.0 of 4 people",
    "stderr" : "",
    "strace" : "execve(\"/bin/sh\", [\"sh\", \"-c\", \"python /tmp/code/demo1_code/run.\"...], 0x7ffe8a712338 ...."
  }
}
```

## Debugging container contents

... can be tricky, because the container normally exits very quickly and you can't get a shell inside of it anymore. Luckily, there is an easy solution:

* Add `sleep 1000` in the script `tracerun.sh`
* Check the container ID in the output of the tasker
* `docker exec -it <container-id> bash` and you are in
* If you kill tasker before the script finishes its sleep, delete the container manually.

## Deployment
* Install JRE 11;
* Install Docker;
* Install and start RabbitMQ docker container;
* Package: `mill tasker.assembly`;
* Copy the `out/tasker/assembly/dest/out.jar` wherever need;
* Run it with `java -Djava.io.tmpdir=/tmp -jar ../out.jar`.

## Development

First, generate an Intellij project
```
mill mill.scalalib.GenIdea/idea
```

Then, open the project in IntelliJ IDEA. Install the necessary dependencies when prompted (make sure
to install Scala 2.13.1 when prompted, and not the default 2.12!), then open Tasker.scala and click
the little play icon at the top left in the left gutter.

### 💄 Format code before committing! 
Either [configure IntelliJ to do it on save](https://scalameta.org/scalafmt/docs/installation.html#format-on-save), or run `mill tasker.reformat` in the root of the project.   

### Used libraries

* Cats Effect
* [fs2-rabbit](https://fs2-rabbit.profunktor.dev/guide.html)
