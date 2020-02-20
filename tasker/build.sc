import mill._
import scalalib._
import scalafmt._

object tasker extends ScalaModule with ScalafmtModule {

  val http4sVersion = "0.21.0-M5"
  val circeVersion = "0.12.2"
  val circeOpticsVersion = "0.12.0"
  val pureconfigVersion = "0.12.2"

  object watcher extends ScalaModule with ScalafmtModule {
    override def scalaVersion = tasker.scalaVersion
    override def scalacOptions = tasker.scalacOptions
    override def ivyDeps = Agg(
      ivy"org.http4s::http4s-dsl:$http4sVersion",
      ivy"org.http4s::http4s-blaze-client:$http4sVersion",
      ivy"org.tpolecat::doobie-core:0.8.4",
      ivy"org.tpolecat::doobie-postgres:0.8.4"
    )
    override def moduleDeps = Seq(common)
  }

  object runner extends ScalaModule with ScalafmtModule {
    override def scalaVersion = tasker.scalaVersion
    override def scalacOptions = tasker.scalacOptions
    override def moduleDeps = Seq(common)
  }

  object cacher extends ScalaModule with ScalafmtModule {
    override def scalaVersion = tasker.scalaVersion
    override def scalacOptions = tasker.scalacOptions
    override def moduleDeps = Seq(common)

    override def ivyDeps = Agg(
      ivy"org.http4s::http4s-dsl:$http4sVersion",
      ivy"org.http4s::http4s-circe:$http4sVersion",
      ivy"org.http4s::http4s-blaze-client:$http4sVersion",
      ivy"org.http4s::http4s-blaze-server:$http4sVersion",
      ivy"io.circe::circe-optics:$circeOpticsVersion"
    )
  }

  object common extends ScalaModule with ScalafmtModule {
    override def scalaVersion = tasker.scalaVersion
    override def scalacOptions = tasker.scalacOptions

    override def ivyDeps = Agg(
        ivy"com.github.lookfirst:sardine:5.9",
        ivy"javax.xml.bind:jaxb-api:2.4.0-b180830.0359", // For Sardine
        ivy"javax.activation:activation:1.1", // For Sardine
        ivy"org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438", // For Sardine

        ivy"org.typelevel::cats-effect:2.0.0",

        ivy"dev.profunktor::fs2-rabbit:2.0.0",
        ivy"dev.profunktor::fs2-rabbit-circe:2.0.0",
        ivy"io.circe::circe-generic-extras:$circeVersion",
        ivy"io.circe::circe-generic:$circeVersion",
        ivy"co.fs2::fs2-core:2.0.0",

        ivy"io.chrisdavenport::log4cats-slf4j:1.0.0",
        ivy"ch.qos.logback:logback-classic:1.2.3",

        ivy"com.github.docker-java:docker-java:3.2.0-rc1",

        ivy"com.github.pureconfig::pureconfig:$pureconfigVersion",
        ivy"com.github.pureconfig::pureconfig-cats-effect:$pureconfigVersion"
      )
  }

  override def scalaVersion = "2.13.1"
  override def scalacOptions = Seq("-deprecation", "-Xlint", "-Xfatal-warnings")
  override def moduleDeps = Seq(runner, watcher, cacher)
  override def forkArgs = Seq("-Djava.io.tmpdir=/tmp/tasker")
}
