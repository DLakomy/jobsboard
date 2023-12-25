package dlakomy.jobsboard.http.routes

import cats.effect.*
import cats.syntax.all.*
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.domain.job.*
import dlakomy.jobsboard.domain.pagination.*
import dlakomy.jobsboard.domain.security.*
import dlakomy.jobsboard.http.responses.*
import dlakomy.jobsboard.http.validation.syntax.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.authentication.asAuthed

import java.util.UUID
import scala.language.implicitConversions


class JobRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (jobs: Jobs[F], stripe: Stripe[F])
    extends HttpValidationDsl[F]:

  object LimitQueryParam  extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")

  // GET /jobs/filters => { filters }
  private val allFiltersRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case GET -> Root / "filters" =>
      jobs.possibleFilters().flatMap(Ok(_))

  // POST /jobs?limit==x&offset=y { filters }
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) =>
      for
        filter   <- req.as[JobFilter]
        jobsList <- jobs.all(filter, Pagination(limit, offset))
        resp     <- Ok(jobsList)
      yield resp

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case GET -> Root / UUIDVar(id) =>
      jobs
        .find(id)
        .flatMap:
          case Some(job) => Ok(job)
          case None      => NotFound(FailureResponse(s"Job $id not found"))

  // POST /jobs { jobInfo }
  private val createJobRoute: AuthRoute[F] =
    case req @ POST -> Root / "create" asAuthed user =>
      req.request.withValidated[JobInfo]: jobInfo =>
        for
          jobId <- jobs.create(user.email, jobInfo)
          resp  <- Created(jobId)
        yield resp

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: AuthRoute[F] =
    case req @ PUT -> Root / UUIDVar(id) asAuthed user =>
      req.request.withValidated[JobInfo]: jobInfo =>
        jobs
          .find(id)
          .flatMap:
            case None =>
              NotFound(FailureResponse(s"Cannot update job $id: not found"))
            case Some(job) if user.owns(job) || user.isAdmin =>
              jobs.update(id, jobInfo) *> Ok()
            case _ =>
              Forbidden(FailureResponse("You can only update your own jobs"))

  // DELETE /jobs/uuid
  private val deleteJobRoute: AuthRoute[F] =
    case req @ DELETE -> Root / UUIDVar(id) asAuthed user =>
      jobs
        .find(id)
        .flatMap:
          case None =>
            NotFound(FailureResponse(s"Cannot delete job $id: not found"))
          case Some(job) if user.owns(job) || user.isAdmin =>
            jobs.delete(id) *> Ok()
          case _ =>
            Forbidden(FailureResponse("You can only delete your own jobs"))

  ////////// stripe endpoints
  // POST /jobs/promoted { jobInfo } => payment link
  private val createJobRoutePromoted: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ POST -> Root / "promoted" =>
      req.withValidated[JobInfo]: jobInfo =>
        for
          jobId   <- jobs.create("TODO@lakomy.com", jobInfo)
          session <- stripe.createCheckoutSession(jobId.toString, "TODO@lakomy.com") // TODO
          resp    <- session.map(sesh => Ok(sesh.getUrl)).getOrElse(NotFound())
        yield resp

  private val authedRoutes =
    SecuredHandler[F].liftService(
      createJobRoute.restrictedTo(allRoles) |+| deleteJobRoute.restrictedTo(allRoles) |+| updateJobRoute.restrictedTo(
        allRoles
      )
    )
  private val unauthedRoutes = allJobsRoute <+> findJobRoute <+> allFiltersRoute <+> createJobRoutePromoted

  val routes = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )


object JobRoutes:
  def apply[F[_]: Concurrent: Logger: SecuredHandler](jobs: Jobs[F], stripe: Stripe[F]) =
    new JobRoutes[F](jobs, stripe)
