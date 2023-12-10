package dlakomy.jobsboard.core

import cats.effect.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import org.testcontainers.containers.PostgreSQLContainer


trait DoobieSpec:

  // to be implemented by the test case that interacts with the DB
  val initScript: String

  val postgres: Resource[IO, PostgreSQLContainer[Nothing]] =
    val acquire = IO:
      val container = new PostgreSQLContainer("postgres")
      container.withInitScript(initScript)
      container.start()
      container

    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
    Resource.make(acquire)(release)
  end postgres

  val transactor: Resource[IO, Transactor[IO]] = for
    db <- postgres
    ce <- ExecutionContexts.fixedThreadPool[IO](1)
    xa <- HikariTransactor
      .newHikariTransactor[IO]("org.postgresql.Driver", db.getJdbcUrl(), db.getUsername(), db.getPassword(), ce)
  yield xa
