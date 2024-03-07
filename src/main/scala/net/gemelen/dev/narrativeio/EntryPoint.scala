package net.gemelen.dev.narrativeio

import net.gemelen.dev.narrativeio.conf.HttpConf
import net.gemelen.dev.narrativeio.conf.QuestdbConf
import net.gemelen.dev.narrativeio.data.EmbeddedDatastore
import net.gemelen.dev.narrativeio.datastore.PixelRepository
import net.gemelen.dev.narrativeio.services.Analytics
import net.gemelen.dev.narrativeio.services.PixelStore

import cats.effect.*
import cats.effect.kernel.Resource
import cats.effect.std.Semaphore
import cats.effect.std.Supervisor
import com.comcast.ip4s.*
import fs2.io.net.Network
import io.circe.config.parser
import io.circe.generic.auto.*
import io.questdb.cairo.CairoEngine
import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object EntryPoint extends IOApp {

  def prepareServer[F[_]: Async: Network](conf: HttpConf, logger: SelfAwareStructuredLogger[F], router: HttpApp[F]) = {
    EmberServerBuilder
      .default[F]
      .withHttpApp(router)
      .withHost(Host.fromString(conf.host).getOrElse(ipv4"0.0.0.0"))
      .withPort(Port.fromInt(conf.port).getOrElse(port"8888"))
      .withLogger(logger)
      .build
  }

  def prepareRouter[F[_]: Async](repo: PixelRepository[F]) =
    AppRouter[F](analyticsService = Analytics[F](repo), storeService = PixelStore[F](repo))

  override def run(args: List[String]): IO[ExitCode] = {

    val res = for {
      logger   <- Resource.eval(Slf4jLogger.create[IO])
      httpConf <- Resource.eval(parser.decodePathF[IO, HttpConf]("server"))
      dbConf   <- Resource.eval(parser.decodePathF[IO, QuestdbConf]("questdb"))
      lock     <- Resource.eval(Semaphore[IO](1L))
      manager  <- Supervisor[IO](await = true)
    } yield (logger, httpConf, dbConf, lock, manager)

    def effect(logger: SelfAwareStructuredLogger[IO], lock: Semaphore[IO], manager: Supervisor[IO], httpConf: HttpConf)(
        engine: CairoEngine
    ): Resource[IO, Server] = {
      PixelRepository[IO](engine = engine, semaphore = lock, supervisor = manager).flatMap { repo =>
        prepareServer[IO](httpConf, logger, prepareRouter[IO](repo))
      }
    }

    val exitCode = res
      .use { case (logger, httpConf, dbConf, lock, manager) =>
        val server: Resource[IO, Server] = EmbeddedDatastore[IO](dbConf.dataDir)
          .flatMap { engine =>
            effect(logger, lock, manager, httpConf)(engine)
          }
        server.useForever.as(ExitCode.Success)
      }

    exitCode
  }
}
