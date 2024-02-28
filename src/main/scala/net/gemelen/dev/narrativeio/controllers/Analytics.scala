package net.gemelen.dev.narrativeio.controllers

import net.gemelen.dev.narrativeio.*
import net.gemelen.dev.narrativeio.domain.*
import net.gemelen.dev.narrativeio.validators.InputValidator
import net.gemelen.dev.narrativeio.validators.InputValidator.given

import cats.Show
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.kernel.Sync
import cats.implicits.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import cats.syntax.show.toShow
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Analytics {

  object TimeStamp extends QueryParamDecoderMatcher[Long]("timestamp")
  object User      extends QueryParamDecoderMatcher[Int]("user")
  object Event     extends QueryParamDecoderMatcher[String]("event")

  def controller[F[_]: Sync](
      analyticsService: services.Analytics[F],
      storeService: services.PixelStore[F]
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root :? TimeStamp(timestamp) =>
        for {
          summary <- analyticsService.summarise(timestamp)
          resp    <- Ok(AnalyticsResponse(summary).show)
        } yield (resp)
      case POST -> Root
          :? TimeStamp(timestamp)
          +& User(user)
          +& Event(event) =>
        val x = InputValidator(timestamp, user, event)
        x match {
          case Valid(a)   =>
          case Invalid(e) =>
        }
        for {
          logger <- Slf4jLogger.create[F]
          _ <- InputValidator(timestamp, user, event) match {
            case Valid(a) => storeService.store(a)
            case Invalid(e) =>
              val errorPart = if (e.size > 1) "Errors are" else "Error is"
              logger.warn(
                s"Input data was dropped due to the validation error. Input is: (timestamp = $timestamp, user_id = $user, event = $event). $errorPart: ${e.show}"
              )
          }
          resp <- NoContent()
        } yield (resp)
    }

  }

  case class AnalyticsResponse(output: Output)
  object AnalyticsResponse {
    given Show[AnalyticsResponse] = Show.show[AnalyticsResponse] { resp =>
      s"""unique_users,${resp.output.unique_users}
         |clicks,${resp.output.clicks}
         |impressions,${resp.output.impressions}
         |""".stripMargin
    }
  }
}
