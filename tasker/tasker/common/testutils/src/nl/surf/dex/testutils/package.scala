package nl.surf.dex

import cats.effect.{ContextShift, IO, Resource}
import better.files.{File => BFile}

import scala.concurrent.{ExecutionContext, Future}

package object testutils {

  def withTmpDir[X](thunk: BFile => IO[X]) =
    Resource
      .make(acquire = IO(BFile.newTemporaryDirectory("dex")))(
        release = t => IO(t.delete())
      )
      .use(thunk)

  def appendBytesOfSize(file: BFile, sizeBytes: Int): IO[BFile] =
    IO(file.appendByteArray(Array.fill(sizeBytes) { 0.byteValue }))

  object implicits {
    implicit def IOunsafeToFuture[X](thunk: IO[X]): Future[X] =
      thunk.unsafeToFuture()

    implicit val cs: ContextShift[IO] =
      IO.contextShift(ExecutionContext.global)

  }

}
