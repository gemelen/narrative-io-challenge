package net.gemelen.dev.narrativeio.datastore

import net.gemelen.dev.narrativeio.domain.Input
import net.gemelen.dev.narrativeio.domain.Pixel

import scala.util.Try

import cats.effect.kernel.Resource
import cats.effect.kernel.Sync
import cats.effect.std.Semaphore
import cats.effect.std.Supervisor
import cats.syntax.applicative.catsSyntaxApplicativeId
import cats.syntax.functor.toFunctorOps
import io.questdb.cairo.CairoEngine
import io.questdb.cairo.security.AllowAllSecurityContext
import io.questdb.griffin.SqlExecutionContextImpl

trait PixelRepository[F[_]] {
  def save(pixel: Input): F[Unit]
  def countEventsWithinHour(timestamp: Long, event: Pixel): F[Long]
  def countUsersWithinHour(timestamp: Long): F[Long]
}

object PixelRepository {

  def apply[F[_]: Sync](
      engine: CairoEngine,
      semaphore: Semaphore[F],
      supervisor: Supervisor[F]
  ): Resource[F, PixelRepository[F]] = {

    def acquire: F[PixelRepository[F]] = {
      val F = Sync[F]
      lazy val sqlContext =
        new SqlExecutionContextImpl(engine, 1).`with`(AllowAllSecurityContext.INSTANCE, null)

      def createTable: F[Unit] = {
        val sql =
          "CREATE TABLE IF NOT EXISTS pixel(event SYMBOL, user_id LONG, ts TIMESTAMP) TIMESTAMP(ts) PARTITION BY HOUR;"
        supervisor.supervise {
          semaphore.permit.use[Unit](_ => engine.ddl(sql, sqlContext).pure[F])
        }.void
      }

      /*
       * This fiber management seems to be an overkill, since QuestDB manages concurrent access on its own,
       * but I'd do it anyway for the sake of the exercise.
       */
      val repository = new PixelRepository[F] {
        override def countUsersWithinHour(timestamp: Long): F[Long] = {
          F.interruptible {
            val factory = engine.select(
              s"SELECT COUNT_DISTINCT(user_id) FROM pixel WHERE ts IN TO_STR(DATE_TRUNC('hour',CAST($timestamp AS DATE)), 'yyyy-MM-ddTHH');",
              sqlContext
            )
            val cursor = factory.getCursor(sqlContext)
            val count = if (cursor.hasNext()) {
              cursor.getRecord().getLong(1)
            } else {
              -1L
            }
            cursor.close()

            count
          }
        }

        override def countEventsWithinHour(timestamp: Long, event: Pixel): F[Long] = {
          F.interruptible {
            val sql = event match {
              case Pixel.Click =>
                s"select count() from pixel where ts in to_str(date_trunc('hour',CAST($timestamp AS DATE)), 'yyyy-MM-ddTHH') and event = 'click';"
              case Pixel.Impression =>
                s"select count() from pixel where ts in to_str(date_trunc('hour',CAST($timestamp AS DATE)), 'yyyy-MM-ddTHH') and event = 'impression';"
            }
            val factory = engine.select(sql, sqlContext)
            val cursor  = factory.getCursor(sqlContext)
            val count = if (cursor.hasNext()) {
              cursor.getRecord().getLong(1)
            } else {
              -1L
            }
            cursor.close()
            count
          }
        }

        override def save(pixel: Input): F[Unit] = {
          val sql =
            s"insert into pixel (ts, user_id, event) values (CAST(${pixel.timestamp
                .toEpochMilli()} AS DATE), ${pixel.user.id}, '${pixel.event.event}');"
          /*
           * Starts a new fiber via Supervisor, uses Semaphore as a resource, releasing after effect has been run
           * */
          supervisor.supervise {
            semaphore.permit.use[Unit](_ => engine.insert(sql, sqlContext).pure[F])
          }.void
        }
      }

      F.*>(createTable)(repository.pure[F])
    }
    def release(repository: PixelRepository[F]): F[Unit] = {
      val F = Sync[F]
      lazy val sqlContext =
        new SqlExecutionContextImpl(engine, 1).`with`(AllowAllSecurityContext.INSTANCE, null)
      def dropTable: F[Unit] = {
        val sql =
          "DROP TABLE IF EXISTS pixel;"
        supervisor.supervise {
          semaphore.permit.use[Unit](_ => F.*>(Try(engine.ddl(sql, sqlContext)).pure[F])(F.unit))
        }.void
      }

      F.*>(dropTable)(Sync[F].unit)
    }

    Resource.make(acquire)(release)
  }

}
