package net.gemelen.dev.narrativeio.services

import net.gemelen.dev.narrativeio.datastore.PixelRepository
import net.gemelen.dev.narrativeio.domain.Input

import cats.effect.kernel.Async

trait PixelStore[F[_]: Async] extends Service[F] {
  def store(record: Input): F[Unit]
}

object PixelStore {
  def apply[F[_]: Async](pixelRepo: PixelRepository[F]): PixelStore[F] = new PixelStore[F] {
    override def store(record: Input): F[Unit] = pixelRepo.save(record)
  }
}
