package dlakomy.jobsboard.http.routes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.domain.job.*
import dlakomy.jobsboard.domain.pagination.Pagination
import dlakomy.jobsboard.fixtures.*
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID


class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture
    with SecuredRouteFixture:

  ////////////////////////////////////////////////////////////
  // PREP
  ////////////////////////////////////////////////////////////
  val jobs: Jobs[IO] = new Jobs[IO]:
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] =
      IO.pure(NewJobUuid)

    override def all(): fs2.Stream[IO, Job] =
      fs2.Stream.emit(AwesomeJob)

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if (filter.remoteOnly) IO.pure(List())
      else IO.pure(List(AwesomeJob))

    override def find(id: UUID): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        IO.pure(Some(AwesomeJob))
      else
        IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        IO.pure(Some(UpdatedAwesomeJob))
      else
        IO.pure(None)

    override def delete(id: UUID): IO[Int] =
      if (id == AwesomeJobUuid)
        IO.pure(1)
      else
        IO.pure(0)

    override def possibleFilters(): IO[JobFilter] =
      IO(defaultFilter)

    override def activate(id: UUID): IO[Int] = IO.pure(1)
  end jobs

  val stripe: Stripe[IO] = new Stripe[IO]:
    override def createCheckoutSession(jobId: String, userEmail: String): IO[Option[Session]] =
      IO.pure(Some(Session.create(SessionCreateParams.builder().build())))

    override def handleWebhookEvent[A](payload: String, signature: String, action: String => IO[A]): IO[Option[A]] =
      IO.pure(None)

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  // this is what we are testing
  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs, stripe).routes
  val defaultFilter: JobFilter  = JobFilter(companies = List("Awesome Company"))
  ////////////////////////////////////////////////////////////
  // TESTS
  ////////////////////////////////////////////////////////////

  "JobRoutes" - {
    "should return a job with a given id" in:
      for
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        retrieved <- response.as[Job]
      yield
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomeJob

    "should return all jobs" in:
      for
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter())
        )
        retrieved <- response.as[List[Job]]
      yield
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)

    "should return all jobs that satisfy a filter" in:
      for
        jwtToken <- mockedAuthenticator.create(dawidEmail)
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter(remoteOnly = true))
            .withBearerToken(jwtToken)
        )
        retrieved <- response.as[List[Job]]
      yield
        response.status shouldBe Status.Ok
        retrieved shouldBe List.empty

    "should create a new job" in:
      for
        jwtToken <- mockedAuthenticator.create(dawidEmail)
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        retrieved <- response.as[UUID]
      yield
        response.status shouldBe Status.Created
        retrieved shouldBe NewJobUuid

    "should only update a job that exists" in:
      for
        jwtToken <- mockedAuthenticator.create(dawidEmail)
        responseOk <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
      yield
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound

    "should forbid the update of a job that JWT token doesn't 'own'" in:
      for
        jwtToken <- mockedAuthenticator.create(
          johnEmail
        )
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
      yield response.status shouldBe Status.Forbidden

    "should only delete a job that exists" in:
      for
        jwtToken <- mockedAuthenticator.create(dawidEmail)
        responseOk <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withBearerToken(jwtToken)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withBearerToken(jwtToken)
        )
      yield
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound

    "should surface all possible filters" in:
      for
        response <- jobRoutes.orNotFound.run(Request(method = Method.GET, uri = uri"/jobs/filters"))
        filters  <- response.as[JobFilter]
      yield filters shouldBe defaultFilter
  }
