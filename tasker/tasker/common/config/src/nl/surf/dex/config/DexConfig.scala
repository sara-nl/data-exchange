package nl.surf.dex.config

import java.util.concurrent.Executors

import cats.effect.{Blocker, IO, Resource}
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

/**
  * This is the base class for all config object in DEX
  */
abstract class DexConfig(protected val namespace: String) {

  protected val blocker = Resource
    .make(IO(Executors.newCachedThreadPool()))(es => IO(es.shutdown()))
    .map(Blocker.liftExecutorService)

  def configSrc: CatsEffectConfigSource = ConfigSource.default.at(namespace)

}
