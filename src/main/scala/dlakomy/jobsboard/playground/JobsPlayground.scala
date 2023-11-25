package dlakomy.jobsboard.playground

import cats.effect.*
import cats.implicits.*
import dlakomy.jobsboard.core.LiveJobs
import dlakomy.jobsboard.domain.job.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.util.*


object JobsPlayground extends IOApp.Simple:

  val postgresResource: Resource[IO, HikariTransactor[IO]] = for
    ec <- ExecutionContexts.fixedThreadPool[IO](32)
    xa <- HikariTransactor
      .newHikariTransactor[IO]("org.postgresql.Driver", "jdbc:postgresql:board", "docker", "docker", ec)
  yield xa

  val jobInfo = JobInfo.minimal(
    company = "Dlakomy Corp.",
    title = "Git-man",
    description = "Git expert",
    externalUrl = "gugle.com",
    remote = true,
    location = "Anywhere"
  )

  override def run: IO[Unit] = postgresResource.use: xa =>
    for
      jobs      <- LiveJobs[IO](xa)
      _         <- IO.println("Ready. Next...") >> IO.readLine
      id        <- jobs.create("dawid@lakomy.pl", jobInfo)
      _         <- IO.println("Created job. Next...") >> IO.readLine
      list      <- jobs.all()
      _         <- IO.println(s"All jobs: $list. Next...") >> IO.readLine
      _         <- jobs.update(id, jobInfo.copy(title = "Grand git-man"))
      newJob    <- jobs.find(id)
      _         <- IO.println(s"Job after update: $newJob. Next...") >> IO.readLine
      _         <- jobs.delete(id)
      listAfter <- jobs.all()
      _         <- IO.println(s"Deleted job. List now: $listAfter. Next...") >> IO.readLine
    yield ()
