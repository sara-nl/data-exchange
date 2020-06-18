import dex.versions.scalatest
import mill._
import mill.api.Loose
import mill.define.Target
import mill.scalalib.scalafmt._
import mill.scalalib.{ScalaModule, _}

object dex {
  trait DexModule extends ScalaModule with ScalafmtModule {

  override def scalacPluginIvyDeps: Target[Loose.Agg[Dep]] = Agg(
    ivy"org.typelevel:::kind-projector:0.11.0",
    ivy"com.olegpy::better-monadic-for:0.3.1"
  )

    override def scalaVersion = "2.13.1"
    override def scalacOptions = Seq("-feature", "-language:reflectiveCalls", "-language:implicitConversions", "-deprecation", "-Xlint", "-Xfatal-warnings")
    override def forkArgs = Seq("-Djava.io.tmpdir=/tmp/tasker")


    trait DexTests extends Tests {

      override def ivyDeps = Agg(
        ivy"org.scalatest::scalatest:$scalatest",
        ivy"org.typelevel::cats-testkit-scalatest:1.0.1"
      )
      def testFrameworks =  Seq("org.scalatest.tools.Framework")
    }

  }

  object versions {
    val http4s = "0.21.3"
    val circe = "0.12.2"
    val circeOptics = "0.12.0"
    val pureconfig = "0.12.3"
    val fs2Core = "2.3.0"
    val doobie = "0.9.0"
    val scalatest = "3.1.2"
    val catsEffect = "2.1.3"
    val scalaUri = "2.2.2"
    val betterFiles = "3.9.1"
  }

}

object tasker extends dex.DexModule {
  import dex.versions._

  override def moduleDeps = Seq(runner, watcher, cacher, figurer)

  object watcher extends dex.DexModule {
    override def ivyDeps = Agg(
      ivy"org.http4s::http4s-dsl:$http4s",
      ivy"org.http4s::http4s-blaze-client:$http4s"
    )
    override def moduleDeps = Seq(common, common.messaging, common.database, common.storage)
  }

  object runner extends dex.DexModule {
    override def scalacOptions = tasker.scalacOptions
    override def moduleDeps = Seq(common, common.messaging, common.storage)
  }

  object figurer extends dex.DexModule {
    override def moduleDeps = Seq(common, common.messaging, common.database, common.storage)
    override def ivyDeps = Agg(ivy"org.python:jython-slim:2.7.2")
    override def scalacPluginIvyDeps = tasker.scalacPluginIvyDeps

    object test extends DexTests
  }

  object cacher extends dex.DexModule {
    override def moduleDeps = Seq(common, common.storage)

    override def ivyDeps = Agg(
      ivy"org.http4s::http4s-dsl:$http4s",
      ivy"org.http4s::http4s-circe:$http4s",
      ivy"org.http4s::http4s-blaze-client:$http4s",
      ivy"org.http4s::http4s-blaze-server:$http4s",
      ivy"io.circe::circe-optics:$circeOptics",
      ivy"io.github.mkotsur::artc:0.1.0"
    )
  }

  object common extends dex.DexModule {

    override def ivyDeps = Agg(
      ivy"org.typelevel::cats-effect:$catsEffect",
      ivy"co.fs2::fs2-core:$fs2Core",
      ivy"io.circe::circe-generic-extras:$circe",
      ivy"io.circe::circe-generic:$circe",
      ivy"com.github.pathikrit::better-files:$betterFiles",
      ivy"io.chrisdavenport::log4cats-slf4j:1.0.0",
      ivy"ch.qos.logback:logback-classic:1.2.3",
      ivy"com.github.docker-java:docker-java:3.2.0-rc1"
    )

    override def moduleDeps = Seq(common.config)

    object messaging extends dex.DexModule {
      override def ivyDeps = Agg(
        ivy"org.typelevel::cats-effect:2.0.0",
        ivy"dev.profunktor::fs2-rabbit:2.1.1",
        ivy"dev.profunktor::fs2-rabbit-circe:2.1.1",
        ivy"io.circe::circe-generic-extras:$circe",
        ivy"io.circe::circe-generic:$circe",
        ivy"co.fs2::fs2-core:$fs2Core",
        ivy"com.github.pathikrit::better-files:$betterFiles",
        ivy"io.chrisdavenport::log4cats-slf4j:1.0.0",
        ivy"ch.qos.logback:logback-classic:1.2.3"
      )

      override def moduleDeps = Seq(common, common.config)

    }

    object database extends dex.DexModule {
      override def moduleDeps = Seq(common)
      override def ivyDeps = Agg(
        ivy"org.tpolecat::doobie-core:$doobie",
        ivy"org.tpolecat::doobie-postgres:$doobie"
      )
    }

    object storage extends dex.DexModule {
      override def moduleDeps = Seq(common.config)

      override def ivyDeps = Agg(
        ivy"com.github.lookfirst:sardine:5.9",
        ivy"javax.xml.bind:jaxb-api:2.4.0-b180830.0359", // For Sardine
        ivy"javax.activation:activation:1.1", // For Sardine
        ivy"org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438", // For Sardine
        ivy"org.typelevel::cats-effect:$catsEffect",
        ivy"co.fs2::fs2-core:$fs2Core",
        ivy"io.chrisdavenport::log4cats-slf4j:1.0.0",
        ivy"ch.qos.logback:logback-classic:1.2.3",
        ivy"io.lemonlabs::scala-uri:$scalaUri",
        ivy"com.github.pathikrit::better-files:$betterFiles"
      )

      object test extends DexTests
    }

    object config extends dex.DexModule {
      override def ivyDeps = Agg(
        ivy"com.github.pureconfig::pureconfig:$pureconfig",
        ivy"com.github.pureconfig::pureconfig-cats-effect:$pureconfig"
      )
    }

  }

}
