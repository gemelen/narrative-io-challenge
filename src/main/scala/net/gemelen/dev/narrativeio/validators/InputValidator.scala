package net.gemelen.dev.narrativeio.validators

import net.gemelen.dev.narrativeio.domain.*

import java.time.Instant

import cats.Show
import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.implicits.catsSyntaxTuple3Semigroupal

trait InputValidator[A] {
  def validate(input: A): Validated[ValidationError, A]
}

object InputValidator {

  given Show[ValidationError] = Show.show[ValidationError](_.errorMessage)
  given Show[NonEmptyList[ValidationError]] =
    Show.show[NonEmptyList[ValidationError]](nel => s"(${nel.toList.mkString(",")})")

  def apply(input: (Long, Long, String)): ValidatedNel[ValidationError, Input] = {
    val (ts, id, kind) = input
    (
      timestampValidator.validate(ts).toValidatedNel,
      userIdValidator.validate(id).toValidatedNel,
      eventTypeValidator.validate(kind).toValidatedNel
    ).mapN { case (tsV, idV, kindV) =>
      Input(
        timestamp = Instant.ofEpochMilli(tsV),
        user = UserId(idV),
        event = kindV match {
          case "click"      => Pixel.Click
          case "impression" => Pixel.Impression
        }
      )
    }
  }

  val timestampValidator = new InputValidator[Long] {
    override def validate(input: Long): Validated[ValidationError, Long] = {
      if (input >= 0) Valid(input) else Invalid(TimestampIsOutOfRange)
    }
  }

  val userIdValidator = new InputValidator[Long] {
    override def validate(input: Long): Validated[ValidationError, Long] = {
      if (input > 0) Valid(input) else Invalid(UserIdIsOutOfRange)
    }
  }

  val eventTypeValidator = new InputValidator[String] {
    override def validate(input: String): Validated[ValidationError, String] = {
      input.toLowerCase() match {
        case "click" | "impression" => Valid(input.toLowerCase())
        case _                      => Invalid(EventIsUnknown)
      }
    }
  }

}
