package net.gemelen.dev.narrativeio.data

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.applicative.catsSyntaxApplicativeId
import fs2.io.file.Files
import fs2.io.file.Path
import io.questdb.cairo.CairoEngine
import io.questdb.cairo.DefaultCairoConfiguration

object EmbeddedDatastore {

  def apply[F[_]: Files: Async, A](
      path: String
  )(nestedEffect: CairoEngine => F[A]): Resource[F, CairoEngine] = {
    val F = Async[F]

    val tmpPath = Files[F].createTempDirectory(Some(Path(path)), "questdb", None)

    def engineAcquire(): F[CairoEngine] = {
      F.flatMap(tmpPath) { p =>
        val engine = new CairoEngine(new DefaultCairoConfiguration(p.toString))
        val usage  = Resource.eval(nestedEffect.apply(engine)).use[Nothing](_ => F.never)
        F.*>(usage)(engine.pure[F])
      }
    }

    def engineRelease(engine: CairoEngine): F[Unit] = {
      F.*>(F.pure { engine.close() })(F.flatMap(tmpPath)(Files[F].deleteRecursively))
    }

    Resource.make[F, CairoEngine](engineAcquire())(engineRelease)
  }

}
