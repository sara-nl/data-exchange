import mill._, scalalib._

object tasker extends ScalaModule {

  def scalaVersion = "2.13.1"

  override def mainClass = Some("Tasker")

  override def forkArgs = Seq("-Djava.io.tmpdir=/tmp")

  override def ivyDeps = Agg(
    ivy"org.typelevel::cats-effect:2.0.0",
    ivy"dev.profunktor::fs2-rabbit:2.0.0",
    ivy"dev.profunktor::fs2-rabbit-circe:2.0.0",
    ivy"co.fs2::fs2-core:2.0.0",
    ivy"org.slf4j:slf4j-simple:2.0.0-alpha1",
    ivy"com.github.docker-java:docker-java:3.1.5",
    ivy"javax.activation:activation:1.1.1",
    ivy"com.github.lookfirst:sardine:5.9"
  )

}

