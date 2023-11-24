package dlakomy.jobsboard

import cats.effect.IO
import cats.effect.IOApp
import dlakomy.jobsboard.config.Config
import dlakomy.jobsboard.config.syntax.*
import dlakomy.jobsboard.http.HttpApi
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger


object Application extends IOApp.Simple:

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run =
    ConfigSource.default
      .loadF[IO, Config]
      .flatMap: config =>
        EmberServerBuilder
          .default[IO]
          .withHost(config.host)
          .withPort(config.port)
          .withHttpApp(HttpApi[IO].routes.orNotFound)
          .build
          .useForever
