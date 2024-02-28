package net.gemelen.dev.narrativeio.domain

import java.time.Instant

import cats.Show

case class Input(timestamp: Instant, user: UserId, event: Pixel)
object Input {

  given Show[Input] = Show.show[Input](input =>
    s"Input(timestamp = ${input.timestamp.toEpochMilli()}, user_id = ${input.user.id}), event = ${input.event}"
  )

}
