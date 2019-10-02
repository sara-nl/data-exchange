import mill._, scalalib._

object tasker extends ScalaModule {

  def scalaVersion = "2.13.1"

  override def ivyDeps = Agg(
//    ivy"com.typesafe.akka:akka-actor:2.5.25",
    ivy"org.typelevel::cats-effect:2.0.0",
    ivy"dev.profunktor::fs2-rabbit:2.0.0",
    ivy"dev.profunktor::fs2-rabbit-circe:2.0.0",
    ivy"co.fs2::fs2-core:2.0.0",
    ivy"org.slf4j:slf4j-simple:2.0.0-alpha1"
  )

}

