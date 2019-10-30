import mill._
import scalalib._
import scalafmt._

object tasker extends ScalaModule with ScalafmtModule {

  object watcher extends ScalaModule with ScalafmtModule {
    override def scalaVersion = tasker.scalaVersion
    override def ivyDeps = Agg(
      ivy"org.http4s::http4s-dsl:0.21.0-M5",
      ivy"org.http4s::http4s-blaze-client:0.21.0-M5"
    )
    override def moduleDeps = Seq(common)
  }

  object runner extends ScalaModule with ScalafmtModule {
    override def scalaVersion = tasker.scalaVersion
    override def moduleDeps = Seq(common)
  }

  object common extends ScalaModule with ScalafmtModule {
    override def scalaVersion = tasker.scalaVersion

    override def ivyDeps = Agg(
        ivy"com.github.lookfirst:sardine:5.9",
        ivy"javax.xml.bind:jaxb-api:2.4.0-b180830.0359", // For Sardine
//        ivy"javax.ws.rs-api:2.1", // For Sardine
        ivy"javax.activation:activation:1.1", // For Sardine
        ivy"org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438", // For Sardine

        ivy"org.typelevel::cats-effect:2.0.0",

        ivy"dev.profunktor::fs2-rabbit:2.0.0",
        ivy"dev.profunktor::fs2-rabbit-circe:2.0.0",
        ivy"co.fs2::fs2-core:2.0.0",

        ivy"io.chrisdavenport::log4cats-core:1.0.0",
        ivy"io.chrisdavenport::log4cats-slf4j:1.0.0",
        ivy"org.slf4j:slf4j-simple:2.0.0-alpha1",

        ivy"com.github.docker-java:docker-java:3.2.0-rc1"
      )
  }

  override def scalaVersion = "2.13.1"
  override def moduleDeps = Seq(runner, watcher)
  override def forkArgs = Seq("-Djava.io.tmpdir=/tmp/tasker")
}
