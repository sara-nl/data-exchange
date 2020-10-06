# Dexter 🎆

Dexter is a family of nano-services, which supports the core functionality of the Data Exchange project.

## Messaging protocol

It uses [RabbitMQ](https://www.rabbitmq.com/) message broker, which implements [AMQP 0.9.1 protocol](https://www.rabbitmq.com/tutorials/amqp-concepts.html).

⚠ AMQP is a programmable protocol, which gives application developers a lot of freedom, so it's a good idea to understand basic concepts of AMQP before changing this service.

See also: [Spike Asynchrony](https://www.notion.so/Spike-Asynchrony-71c015cc8e6645689a16f35b59bd45bb).

Also see in `tasker/common/messaging/src/nl/surf/dex/messaging/Messages.scala`

### Start container (`tasker_todo -> [T]`)

Example:

```json
{
  "taskId": "123",
  "codePath": "kit pes",
  "dataPath": "kitpes_data/cat01.jpeg",
  "codeHash": { "eTag": "5dd2be291447e" }
}
```

### Done (`[T] -> tasker_done`)

The response type has the following structure. Before you judge it for verbocity, think about the following:

- Different states often have very different data associated with them;
- Actual data objects are smaller, as this is a description of all possible combinations of data attributes.

```typescript
type TaskProgress {
    taskId: string
    state: State
}

type State = RejectedStateData |
              RunningStateData |
              SuccessStateData |
              ErrorStateData

type RunningStateData = {
    name: "Running",
    currentStepIndex: number,
    currentStep : {
        name: "VerifyingAlgorithm" |
              "DownloadingFiles"  |
              "InstallingDependencies" |
              "CreatingContainer" |
              "ExecutingAlgorithm" |
              "CleaningUp"
    }
}

type SuccessStateData = {
    name: "Success",
    output: Output
}

type ErrorStateData = {
    name: "Error",
    message: String,
    output: Output,
    failedStep: Step
}

type RejectedStateData = {
    name: "Rejected",
    reason: string
}

type Output {
  stdout: string,
  stderr: string,
  strace: string
}
```

Some examples of JSON:

```json
{
  "taskId": "123",
  "state": {
    "currentStepIndex": 1,
    "currentStep": {
      "name": "DownloadingFiles"
    },
    "name": "Running"
  }
}
```

```json
{
  "taskId": "123",
  "state": {
    "output": {
      "stdout": "Hello, world",
      "stderr": "Oops",
      "strace": "RACESTRACESTRACESTRACEST"
    },
    "name": "Success"
  }
}
```

## Log level

There are loggers configured: for `tasker`, `watcher`, `figurer`, `runner` and `shares`. The log level of those can be changed by adding an appropriate JVM system property. E.g.:

```
-DtaskerLogLevel=info -DwatcherLogLevel=trace -DrunnerLogLevel=debug -DsharesLogLevel=debug
```

Note, that `tasker` is merely a wrapper and most of the stuff will be happening in `watcher`, `runner`, `figurer` and `shares`, so most likely you want to change the level of those loggers.

## Debugging container contents

... can be tricky, because the container normally exits very quickly and you can't get a shell inside of it anymore. Luckily, there is an easy solution:

- Add `sleep 1000` in the script `tracerun.sh`
- Check the container ID in the output of the tasker
- `docker exec -it <container-id> bash` and you are in
- If you kill tasker before the script finishes its sleep, delete the container manually.

## Deployment

- Install JRE 11;
- Install Docker;
- Install and start RabbitMQ docker container;
- Build and install `datex` docker image: `cd container && ./build.sh`
- Package: `mill tasker.assembly`;
- Copy the `out/tasker/assembly/dest/out.jar` wherever need;
- Run it with `java -Djava.io.tmpdir=/tmp -jar ../out.jar`.

## Development

### Running tests

Tests consist of unit and integration tests that access real cloud storage services. In order for them to work properly, you must setup the cloud environment to contain certain resources and provide access credentials.

Please follow [this instruction](tasker/common/storage/ctest/README.md) in order to do that. 

### IDE Setup
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

- Cats Effect
- [fs2-rabbit](https://fs2-rabbit.profunktor.dev/guide.html)
- [doobie: Functional JDBC layer for Scala](https://github.com/tpolecat/doobie)

### Rebuilding and restarting with docker-compose

Assuming everything is running in a single docker-compose session, and you've made your code changes... 

* `docker-compose -f docker-compose.yml -f docker-compose.dev.yml build tasker`
* `docker-compose -f docker-compose.yml -f docker-compose.dev.yml stop tasker`
* `docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d --no-deps tasker`