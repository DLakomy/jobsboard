package dlakomy.jobsboard

import cats.effect.IO
import cats.effect.IOApp
import dlakomy.jobsboard.config.*
import dlakomy.jobsboard.config.syntax.*
import dlakomy.jobsboard.modules.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource


object Application extends IOApp.Simple:

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run = ConfigSource.default
    .loadF[IO, AppConfig]
    .flatMap:
      case AppConfig(postgresConfig, emberConfig, securityConfig, tokenConfig, emailServiceConfig, stripeConfig) =>
        val appResource =
          for
            xa      <- Database.makePostgresResource[IO](postgresConfig)
            core    <- Core[IO](xa, tokenConfig, emailServiceConfig, stripeConfig)
            httpApi <- HttpApi[IO](core, securityConfig)
            server <-
              EmberServerBuilder
                .default[IO]
                .withHost(emberConfig.host)
                .withPort(emberConfig.port)
                .withHttpApp(httpApi.endpoints.orNotFound) // FIXME configure CORS properly
                .build
          yield server

        appResource.useForever
