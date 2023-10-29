package dlakomy.jobsboard.http

import cats.Monad
import cats.syntax.all.*
import dlakomy.jobsboard.http.routes.*
import org.http4s.server.*


class HttpApi[F[_]: Monad] private ():
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes = JobRoutes[F].routes

  val routes = Router("/api" -> (healthRoutes <+> jobRoutes))


object HttpApi:
  def apply[F[_]: Monad] = new HttpApi[F]
