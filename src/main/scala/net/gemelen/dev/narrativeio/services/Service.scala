package net.gemelen.dev.narrativeio.services

import cats.effect.kernel.Async

trait Service[F[_]: Async] {}
