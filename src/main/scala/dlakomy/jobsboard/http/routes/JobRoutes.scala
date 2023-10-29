package dlakomy.jobsboard.http.routes

import cats.Monad
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.server.*


class JobRoutes[F[_]: Monad] private extends Http4sDsl[F]:
  // POST /jobs?offset==x&limit=y { filters } // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case POST -> Root => Ok("TODO allJobsRoute")

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case GET -> Root / UUIDVar(id) =>
      Ok(s"TODO find $id")

  // POST /jobs { jobInfo }
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case POST -> Root =>
      Ok("TODO create")

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case PUT -> Root / UUIDVar(id) =>
      Ok(s"TODO update $id")

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case DELETE -> Root / UUIDVar(id) =>
      Ok(s"TODO delete $id")

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )


object JobRoutes:
  def apply[F[_]: Monad] = new JobRoutes[F]
