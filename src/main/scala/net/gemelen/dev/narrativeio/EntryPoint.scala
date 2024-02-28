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
    ): IO[ExitCode] = {
      PixelRepository[IO](engine = engine, semaphore = lock, supervisor = manager).use { repo =>
        val router = AppRouter[IO](analyticsService = Analytics(repo), storeService = PixelStore[IO](repo))
        val server = prepareServer[IO](httpConf, logger, router)
        server.use(_ => IO.never)
      }
    }

    /*
     *  This is a kinda incorrect workaround for the fact that EmbeddedDatastore resource
     *  should be in use over the http4s server.
     *  This leads to the fact that the whole application isn't cancelable (ie it doesn't
     *  catches SIGTERM (issued by a Ctrl-C keystroke, for example) and required to be
     *  suspended to background in shell (via Ctrl-Z) or killed by SIGKILL right away.
     *  This is an inherently wrong for any production application, but I giving up at this moment
     *  due to the lack of time, sorry for that fact.
     * */
    val exitCode = res
      .use { case (logger, httpConf, dbConf, lock, manager) =>
        EmbeddedDatastore[IO, ExitCode](path = dbConf.dataDir)(
          effect(logger, lock, manager, httpConf)
        ).useForever
      }

    exitCode
  }
}
