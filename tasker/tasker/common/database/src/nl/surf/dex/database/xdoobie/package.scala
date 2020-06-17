package nl.surf.dex.database

import cats.data.NonEmptyList
import doobie.util.Put
import io.circe.Json
import org.postgresql.util.PGobject

/**
  * Extra mappings for Doobie.
  * @see https://tpolecat.github.io/doobie/docs/12-Custom-Mappings.html
  */
package object xdoobie {
  implicit val jsonPut: Put[Json] =
    Put.Advanced.other[PGobject](NonEmptyList.of("json")).tcontramap[Json] {
      j =>
        new PGobject {
          setType("json")
          setValue(j.noSpaces)
        }
    }
}
