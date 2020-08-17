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


    trait DexTests extends Tests with DexModule {
      override def ivyDeps = Agg(
        ivy"org.scalatest::scalatest:${versions.scalatest}",
        ivy"org.typelevel::cats-testkit-scalatest:1.0.1"
      )

      override def forkArgs = Seq("-Djava.io.tmpdir=/tmp")

      override def moduleDeps = super.moduleDeps ++ Seq(tasker.common.testutils)

      def testFrameworks =  Seq("org.scalatest.tools.Framework")
    }

    object versions {
      val http4s = "0.21.3"
      val circe = "0.13.0"
      val circeOptics = "0.12.0"
      val pureconfig = "0.12.3"
      val fs2Core = "2.3.0"
      val doobie = "0.9.0"
      val scalatest = "3.2.0"
      val catsEffect = "2.1.3"
      val scalaUri = "2.2.2"
      val betterFiles = "3.9.1"
    }

    object deps {
      import versions._

      val json = Agg(
        ivy"io.circe::circe-generic:$circe",
        ivy"io.circe::circe-parser:$circe",
        ivy"io.circe::circe-core:$circe",
        ivy"io.circe::circe-generic-extras:0.13.0"
      )

      val files = Agg(
        ivy"io.lemonlabs::scala-uri:$scalaUri",
        ivy"com.github.pathikrit::better-files:$betterFiles"
      )

      val fp = Agg(
        ivy"org.typelevel::cats-effect:$catsEffect",
        ivy"co.fs2::fs2-core:$fs2Core"
      )

      val restClient = Agg(
        ivy"org.http4s::http4s-dsl:$http4s",
        ivy"org.http4s::http4s-circe:$http4s",
        ivy"org.http4s::http4s-blaze-client:$http4s"
      )
    }

  }

}

object tasker extends dex.DexModule {
  import versions._

  override def moduleDeps = Seq(runner, watcher, shares, figurer)

  object watcher extends dex.DexModule {
    override def ivyDeps = Agg(
      ivy"org.http4s::http4s-dsl:$http4s"
    )
    override def moduleDeps = Seq(common, common.messaging, common.database, common.storage.multi)
  }

  object runner extends dex.DexModule {
    override def ivyDeps = Agg(ivy"com.github.docker-java:docker-java:3.2.5")
    override def scalacOptions = tasker.scalacOptions
    override def moduleDeps = Seq(common, common.messaging, common.storage.multi)
  }

  object figurer extends dex.DexModule {
    override def moduleDeps = Seq(common, common.messaging, common.database, common.storage.multi)
    override def ivyDeps = Agg(ivy"org.python:jython-slim:2.7.2")
    override def scalacPluginIvyDeps = tasker.scalacPluginIvyDeps

    object test extends DexTests
  }

  object shares extends dex.DexModule {
    override def moduleDeps = Seq(common, common.storage.multi)

    override def ivyDeps = Agg(
      ivy"io.circe::circe-optics:$circeOptics",
      ivy"org.http4s::http4s-blaze-server:$http4s",
      ivy"io.github.mkotsur::artc:0.1.0"
    ) ++ deps.restClient
  }

  object common extends dex.DexModule {

    override def ivyDeps = Agg(ivy"io.netty:netty-common:4.1.50.Final") ++ deps.fp ++ deps.json ++ deps.files

    override def moduleDeps = Seq(common.config, common.logging)

    object messaging extends dex.DexModule {
      override def ivyDeps = Agg(
        ivy"dev.profunktor::fs2-rabbit:2.1.1",
        ivy"dev.profunktor::fs2-rabbit-circe:2.1.1"
      ) ++ deps.fp ++ deps.files

      override def moduleDeps = Seq(common, config, logging, storage)

      object test extends DexTests
    }

    object logging extends dex.DexModule {
      override def ivyDeps = Agg(
        ivy"io.chrisdavenport::log4cats-slf4j:1.0.0",
        ivy"ch.qos.logback:logback-classic:1.2.3"
      )
    }

    object database extends dex.DexModule {

      override def moduleDeps = Seq(common.config, common.storage)

      override def ivyDeps = Agg(
        ivy"org.tpolecat::doobie-core:$doobie",
        ivy"org.tpolecat::doobie-postgres:$doobie"
      ) ++ deps.fp ++ deps.json
    }

    object storage extends dex.DexModule {

      override def moduleDeps = Seq(common.config)

      override def ivyDeps = deps.files ++ deps.fp ++ deps.json

      object ctest extends DexTests {
        override def moduleDeps = super.moduleDeps ++ Seq(multi)
      }

      object multi extends dex.DexModule {
        override def moduleDeps = Seq(owncloud, gdrive)
      }

      object owncloud extends dex.DexModule {

        override def moduleDeps = Seq(storage, common.config, common.logging)

        override def ivyDeps = Agg(
          ivy"com.github.lookfirst:sardine:5.9",
          ivy"javax.xml.bind:jaxb-api:2.4.0-b180830.0359", // For Sardine
          ivy"javax.activation:activation:1.1", // For Sardine
          ivy"org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438", // For Sardine
          ivy"org.http4s::http4s-dsl:$http4s", // For reading shares
          ivy"org.http4s::http4s-blaze-client:$http4s", // For reading shares
          ivy"io.lemonlabs::scala-uri:$scalaUri"
        ) ++ deps.files ++ deps.fp ++ deps.json ++ deps.restClient

        object test extends DexTests {
          override def forkEnv = T.input {
            Map(
              "RD_WEBDAV_USERNAME_TEST" -> T.ctx.env("RD_WEBDAV_USERNAME_TEST"),
              "RD_WEBDAV_PASSWORD_TEST" -> T.ctx.env("RD_WEBDAV_PASSWORD_TEST")
            )
          }
        }
      }

      object gdrive extends dex.DexModule {
        override def moduleDeps = Seq(storage, common.config, common.logging)

        override def ivyDeps = Agg(
          ivy"com.google.apis:google-api-services-drive:v3-rev20200609-1.30.9",
          ivy"com.google.api-client:google-api-client:1.30.9",
          ivy"com.google.oauth-client:google-oauth-client-jetty:1.30.6",
          ivy"com.google.auth:google-auth-library-oauth2-http:0.21.0"
        ) ++ deps.files ++ deps.fp

        object test extends DexTests
      }

    }

    object config extends dex.DexModule {
      override def ivyDeps = Agg(
        ivy"com.github.pureconfig::pureconfig:$pureconfig",
        ivy"com.github.pureconfig::pureconfig-cats-effect:$pureconfig",
        ivy"io.lemonlabs::scala-uri:$scalaUri"
      )
    }

    object testutils extends dex.DexModule {
      override def ivyDeps = config.deps.files ++ deps.fp
    }

  }

}
