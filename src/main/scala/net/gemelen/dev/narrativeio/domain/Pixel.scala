package net.gemelen.dev.narrativeio.domain

import cats.Show

enum Pixel(val event: String) {
  case Click      extends Pixel("click")
  case Impression extends Pixel("impression")
}

object Pixel {
  given Show[Pixel] = Show.show[Pixel](_.event)
}
