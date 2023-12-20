package dlakomy.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import dlakomy.jobsboard.domain.job.JobFilter
import dlakomy.jobsboard.domain.pagination.Pagination
import dlakomy.jobsboard.fixtures.JobFixture
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


class JobsSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with JobFixture:
  val initScript           = "sql/jobs.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Jobs 'algebra'" - {
    "should return no job if the given UUID does not exist" in:
      transactor.use: xa =>
        val program = for
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.find(NotFoundJobUuid)
        yield retrieved

        program.asserting(_ shouldBe None)

    "should retrieve a job by id" in:
      transactor.use: xa =>
        val program = for
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.find(AwesomeJobUuid)
        yield retrieved

        program.asserting(_ shouldBe Some(AwesomeJob))

    "should retrieve all jobs" in:
      transactor.use: xa =>
        val program = for
          jobs      <- LiveJobs[IO](xa)
          retrieved <- jobs.all()
        yield retrieved

        program.asserting(_ shouldBe List(AwesomeJob))

    "should create a new job" in:
      transactor.use: xa =>
        val program = for
          jobs     <- LiveJobs[IO](xa)
          jobId    <- jobs.create("dawid@dlakomy.github.io", SomeCompanyNewJob)
          maybeJob <- jobs.find(jobId)
        yield maybeJob

        program.asserting(_.map(_.jobInfo) shouldBe Some(SomeCompanyNewJob))

    "should return an updated job" in:
      transactor.use: xa =>
        val program = for
          jobs            <- LiveJobs[IO](xa)
          maybeUpdatedJob <- jobs.update(AwesomeJobUuid, UpdatedAwesomeJob.jobInfo)
        yield maybeUpdatedJob

        program.asserting(_ shouldBe Some(UpdatedAwesomeJob))

    "should return none when trying to update a job that doesn't exist" in:
      transactor.use: xa =>
        val program = for
          jobs            <- LiveJobs[IO](xa)
          maybeUpdatedJob <- jobs.update(NotFoundJobUuid, UpdatedAwesomeJob.jobInfo)
        yield maybeUpdatedJob

        program.asserting(_ shouldBe None)

    "should delete an existing job" in:
      transactor.use: xa =>
        val program = for
          jobs                <- LiveJobs[IO](xa)
          numberOfDeletedJobs <- jobs.delete(AwesomeJobUuid)
          countOfJobs <- sql"SELECT count(1) FROM jobs WHERE id = $AwesomeJobUuid".query[Int].unique.transact(xa)
        yield (numberOfDeletedJobs, countOfJobs)

        program.asserting: (numberOfDeletedJobs, countOfJobs) =>
          numberOfDeletedJobs shouldBe 1
          countOfJobs shouldBe 0

    "should return zero updated rows if the job ID to delete doesn't exist" in:
      transactor.use: xa =>
        val program = for
          jobs                <- LiveJobs[IO](xa)
          numberOfDeletedJobs <- jobs.delete(NotFoundJobUuid)
        yield numberOfDeletedJobs

        program.asserting(_ shouldBe 0)

    "should filter remote jobs" in:
      transactor.use: xa =>
        val program = for
          jobs         <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(remoteOnly = true), Pagination.default)
        yield filteredJobs

        program.asserting(_ shouldBe List.empty)

    "should filter jobs by tags" in:
      transactor.use: xa =>
        val program = for
          jobs         <- LiveJobs[IO](xa)
          filteredJobs <- jobs.all(JobFilter(tags = List("scala", "cats", "whatever")), Pagination.default)
        yield filteredJobs

        program.asserting(_ shouldBe List(AwesomeJob))

    "should give a comprehensive list of filters based on the jobs in the db" in:
      transactor.use: xa =>
        val program = for
          jobs         <- LiveJobs[IO](xa)
          filteredJobs <- jobs.possibleFilters()
        yield filteredJobs

        program.asserting:
          case JobFilter(companies, locations, countries, seniorities, tags, maxSalary, remote) =>
            companies shouldBe List("Awesome Company")
            locations shouldBe List("Berlin")
            countries shouldBe List("Germany")
            seniorities shouldBe List("Senior")
            tags.toSet shouldBe Set("scala", "scala-3", "cats")
            maxSalary shouldBe Some(3000)
  }
