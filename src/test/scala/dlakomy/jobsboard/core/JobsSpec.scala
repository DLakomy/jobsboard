package dlakomy.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import dlakomy.jobsboard.fixtures.JobFixture
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers


class JobsSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with JobFixture:
  val initScript = "sql/jobs.sql"

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
  }
