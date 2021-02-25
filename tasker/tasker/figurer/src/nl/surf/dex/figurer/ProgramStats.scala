package nl.surf.dex.figurer

import cats.data.Kleisli
import cats.effect.IO
import doobie.util.transactor.Transactor
import nl.surf.dex.database.Permissions
import nl.surf.dex.storage.Fileset

import scala.collection.immutable

object ProgramStats {
  // Constructors
  val nothing = ProgramStats(0, 0, 0, Set.empty, Map.empty, Map.empty, 0)

  // Internals
  case class Imports(foundImports: immutable.Set[String], skippedFiles: List[String] = Nil)

  // Persistence
  def storeStats(
      id: Int,
      hash: Fileset.Hash,
      stats: ProgramStats
  ): Kleisli[IO, Transactor[IO], Unit] = {
    import io.circe.generic.auto._
    Permissions.updateStats(id, hash, stats).flatMapF {
      case 1  => IO.unit // success
      case rr => permissionUpdateErrorIO(rr)
    }
  }

  // Errors
  private def permissionUpdateErrorIO(actual: Number) =
    IO.raiseError(
      new IllegalStateException(
        s"Expected to update exactly 1 row(s), but updated $actual"
      )
    )
}

case class ProgramStats(
    lines: Int,
    words: Int,
    chars: Int,
    imports: Set[String],
    skippedImports: Map[String, String],
    //This is probably not a very good idea
    // to store contents of the file here,
    // but since the rest of the app heavily depends on it,
    // keeping things as they are.
    contents: Map[String, String],
    skippedFiles: Int
)
