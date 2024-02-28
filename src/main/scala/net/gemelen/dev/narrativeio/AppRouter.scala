package net.gemelen.dev.narrativeio

import cats.effect.kernel.Sync
import org.http4s.HttpApp
import org.http4s.server.Router

object AppRouter {

  def apply[F[_]: Sync](analyticsService: services.Analytics[F], storeService: services.PixelStore[F]): HttpApp[F] =
    Router.apply {
      "analytics" -> controllers.Analytics.controller[F](analyticsService, storeService)
    }.orNotFound

}
