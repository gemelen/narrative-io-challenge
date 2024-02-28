package net.gemelen.dev.narrativeio.services

import net.gemelen.dev.narrativeio.datastore.PixelRepository
import net.gemelen.dev.narrativeio.domain.Output
import net.gemelen.dev.narrativeio.domain.Pixel

import cats.effect.kernel.Async

trait Analytics[F[_]: Async] extends Service[F] {
  def summarise(since: Long): F[Output]
}

object Analytics {
  def apply[F[_]: Async](pixelRepo: PixelRepository[F]): Analytics[F] = new Analytics[F] {
    override def summarise(since: Long): F[Output] = {
      val F = Async[F]

      val uuF = pixelRepo.countUsersWithinHour(since)
      val cF  = pixelRepo.countEventsWithinHour(since, Pixel.Click)
      val iF  = pixelRepo.countEventsWithinHour(since, Pixel.Impression)

      F.map3(uuF, cF, iF) { case (uu, c, i) =>
        Output(unique_users = uu, clicks = c, impressions = i)
      }
    }
  }
}
