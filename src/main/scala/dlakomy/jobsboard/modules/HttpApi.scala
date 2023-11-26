package dlakomy.jobsboard.modules

import cats.effect.*
import cats.syntax.all.*
import dlakomy.jobsboard.http.routes.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger


class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F]):
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F](core.jobs).routes

  val endpoints = Router("/api" -> (healthRoutes <+> jobRoutes))


object HttpApi:
  def apply[F[_]: Concurrent: Logger](core: Core[F]): Resource[F, HttpApi[F]] = Resource.pure(new HttpApi[F](core))
