package shares.http

import shares.conf.SharesConf.ClientConf
import cats.effect.{ContextShift, IO, Resource}
import nl.surf.dex.storage.owncloud.config.DexResearchDriveConf
import org.http4s.BasicCredentials
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

object HttpClient {

  def credentials(researchDriveConf: DexResearchDriveConf) = {
    BasicCredentials(
      researchDriveConf.webdavUsername,
      researchDriveConf.webdavPassword
    )
  }

  def blazeClientR(
      conf: ClientConf
  )(implicit cs: ContextShift[IO]): Resource[IO, Client[IO]] = {
    BlazeClientBuilder[IO](ExecutionContext.global)
      .withConnectTimeout(conf.connectionTimeout)
      .withResponseHeaderTimeout(conf.responseHeaderTimeout)
      .withRequestTimeout(conf.requestTimeout)
      .withIdleTimeout(conf.idleTimeout)
      .resource
  }

}
