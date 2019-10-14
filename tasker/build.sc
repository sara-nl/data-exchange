import mill._, scalalib._, scalafmt._

object tasker extends ScalaModule with ScalafmtModule {

  def scalaVersion = "2.13.1"

  override def mainClass = Some("Tasker")

  override def forkArgs = Seq("-Djava.io.tmpdir=/tmp")

  override def ivyDeps = Agg(
    ivy"org.typelevel::cats-effect:2.0.0",
    ivy"dev.profunktor::fs2-rabbit:2.0.0",
    ivy"dev.profunktor::fs2-rabbit-circe:2.0.0",
    ivy"co.fs2::fs2-core:2.0.0",
    ivy"org.slf4j:slf4j-simple:2.0.0-alpha1",
    ivy"com.github.docker-java:docker-java:3.2.0-rc1",
    ivy"com.github.lookfirst:sardine:5.9",
    ivy"io.chrisdavenport::log4cats-core:1.0.0",
    ivy"io.chrisdavenport::log4cats-slf4j:1.0.0",
    ivy"javax.xml.bind:jaxb-api:2.4.0-b180830.0359", // For Sardine
    ivy"javax.activation:activation:1.1", // For Sardine
    ivy"org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438" // For Sardine
  )

}

