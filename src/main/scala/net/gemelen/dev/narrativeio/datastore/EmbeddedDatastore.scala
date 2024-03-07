package net.gemelen.dev.narrativeio.data

import scala.util.Try

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import fs2.io.file.Files
import fs2.io.file.Path
import io.questdb.cairo.CairoEngine
import io.questdb.cairo.DefaultCairoConfiguration

object EmbeddedDatastore {

  def apply[F[_]: Files: Async](path: String): Resource[F, CairoEngine] = {
    val F = Async[F]

    val tmpPath = Files[F].createTempDirectory(Some(Path(path)), "questdb", None)

    def engineAcquire(): F[CairoEngine] = {
      F.flatMap(tmpPath) { p => F.blocking(new CairoEngine(new DefaultCairoConfiguration(p.toString))) }
    }

    def engineRelease(engine: CairoEngine): F[Unit] = {
      F.*> { F.blocking { Try(engine.close()) } } { F.flatMap(tmpPath)(Files[F].deleteRecursively) }
    }

    Resource.make[F, CairoEngine](engineAcquire())(engineRelease)
  }

}
