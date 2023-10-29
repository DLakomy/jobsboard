package dlakomy.jobsboard.http.routes

import cats.Monad
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.server.*


class HealthRoutes[F[_]: Monad] private extends Http4sDsl[F]:
  private val healthRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case GET -> Root => Ok("Everything works")

  val routes = Router("/health" -> healthRoute)


object HealthRoutes:
  def apply[F[_]: Monad] = new HealthRoutes[F]
