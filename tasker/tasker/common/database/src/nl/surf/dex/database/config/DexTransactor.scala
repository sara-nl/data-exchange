package nl.surf.dex.database.config

import cats.effect.{ContextShift, IO}
import doobie.util.transactor.Transactor

object DexTransactor {
  def create(dbConf: DbConf)(implicit cs: ContextShift[IO]) =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      dbConf.jdbcUrl,
      dbConf.username,
      dbConf.password
    )
}
