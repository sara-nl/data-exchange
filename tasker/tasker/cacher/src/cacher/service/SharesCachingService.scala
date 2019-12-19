package cacher.service

import cacher.conf.CacherConfig._
import cacher.model.Share.ShareMetadata
import cacher.service.SharesCachingService.IdleRefresh
import cats.effect.concurrent.Ref
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._

object SharesCachingService {

  object IdleRefresh {
    val first: IdleRefresh = IdleRefresh(0)
  }

  case class IdleRefresh private (value: Int) {
    def next: IdleRefresh = copy(value = value + 1)
  }

  def create(implicit cs: ContextShift[IO]): IO[SharesCachingService] =
    for {
      // We set an empty list as initial value and that means, that first to the cache will return a wrong
      // value in case when it happens before the first roundtrip ro ResearchDrive is completed.
      // Alternative (more complex solutions) include:
      // - Introducing a special value (e.g. returning Option[List[Share]], where None means: data not available yet;
      // - Leveraging [[cats.effect.concurrent.Deferred]] datatype.
      sharesRef <- Ref.of[IO, List[ShareMetadata]](Nil)
      refreshRateRef <- Ref.of[IO, IdleRefresh](IdleRefresh.first)
    } yield new SharesCachingService(sharesRef, refreshRateRef)
}

class SharesCachingService(
  val sharesRef: Ref[IO, List[ShareMetadata]],
  currentAttemptRef: Ref[IO, SharesCachingService.IdleRefresh]
) {

  private val logger = Slf4jLogger.getLogger[IO]

  private def backoffInterval(a: SharesCachingService.IdleRefresh) =
    Math
      .min(math.pow(2, a.value).toLong, update.ceilingInterval.toSeconds)
      .seconds

  private def updateOnce(implicit cs: ContextShift[IO]): IO[Unit] =
    SharesService.getShares.attempt.flatMap {
      case Right(newShares) =>
        sharesRef.set(newShares) *> logger.debug(
          s"Updated cache with ${newShares.length} shares from RD"
        )
      case Left(e) =>
        logger.error(e)("Could not fetch shares")
    }

  def reset(implicit cs: ContextShift[IO]): IO[Unit] =
    for {
      _ <- logger.debug(s"Reset refresh interval and update at once")
      _ <- currentAttemptRef.set(IdleRefresh.first)
      _ <- updateOnce
    } yield ()

  def scheduleUpdates(implicit timer: Timer[IO],
                      cs: ContextShift[IO]): IO[Unit] =
    for {
      _ <- updateOnce
      _ <- currentAttemptRef.update(_.next)
      nextAttempt <- currentAttemptRef.get
      _ <- logger.debug(
        s"Scheduling new shares update in ${backoffInterval(nextAttempt)}"
      )
      _ <- timer.sleep(backoffInterval(nextAttempt)) *> scheduleUpdates
    } yield ()
}
