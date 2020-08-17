package watcher

import cats.data.Kleisli
import cats.effect.{IO, Resource}
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.database.{Permissions, Tasks}
import nl.surf.dex.messaging.Messages
import nl.surf.dex.messaging.Messages.StartContainer
import nl.surf.dex.storage.{CloudStorage, FilesetOps}

object Watcher {

  private val logger = Slf4jLogger.getLogger[IO]

  case class Deps(tickStream: fs2.Stream[IO, _],
                  storageFactory: CloudStorage => Resource[IO, FilesetOps],
                  publisher: StartContainer => IO[Unit],
                  xa: Transactor[IO])

  def scheduled: Kleisli[IO, Deps, Unit] = Kleisli {
    case Deps(tickStream, storageFactory, publisher, xa) =>
      tickStream
        .evalTap(_ => logger.debug(s"Fetching eligible permissions from DB"))
        .through(_.flatMap { _ =>
          fs2.Stream
            .evalSeq(Permissions.activeStreamingWithRuns.run(xa))
            .evalTap(
              p =>
                logger.debug(
                  s"Reacting on permission $p \n (already applied for ${p._2.size} datasets"
              )
            )
            .through(DataSet.newDatasetsPipe(storageFactory))
            .evalTap {
              case (newDatasetPath, permission) =>
                for {
                  _ <- logger
                    .info(s"Processing new data set ${newDatasetPath}")
                  taskId <- Tasks
                    .insert(xa)(permission, newDatasetPath)
                  _ <- logger.info(s"Created a new task in the DB $taskId")
                  _ <- publisher(
                    Messages.StartContainer(
                      s"$taskId",
                      permission.dataSet.copy(path = newDatasetPath),
                      permission.algorithm,
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
