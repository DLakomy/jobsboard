package dlakomy.jobsboard.modules

import cats.effect.*
import dlakomy.jobsboard.config.*
import doobie.hikari.HikariTransactor
import doobie.util.*


object Database:
  def makePostgresResource[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] = for
    ec <- ExecutionContexts.fixedThreadPool[F](config.nThreads)
    xa <- HikariTransactor
      .newHikariTransactor[F]("org.postgresql.Driver", config.url, config.user, config.pass, ec)
  yield xa
