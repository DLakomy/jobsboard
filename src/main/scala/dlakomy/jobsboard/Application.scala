package dlakomy.jobsboard

import cats.effect.IO
import cats.effect.IOApp
import dlakomy.jobsboard.config.Config
import dlakomy.jobsboard.config.syntax.*
import dlakomy.jobsboard.http.routes.HealthRoutes
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource


object Application extends IOApp.Simple:

  override def run =
    ConfigSource.default
      .loadF[IO, Config]
      .flatMap: config =>
        EmberServerBuilder
          .default[IO]
          .withHost(config.host)
          .withPort(config.port)
          .withHttpApp(HealthRoutes[IO].routes.orNotFound)
          .build
          .useForever
