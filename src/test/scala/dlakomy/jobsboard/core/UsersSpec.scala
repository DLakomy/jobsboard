package dlakomy.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import dlakomy.jobsboard.domain.user.*
import dlakomy.jobsboard.fixtures.*
import doobie.*
import doobie.implicits.*
import doobie.util.*
import org.postgresql.util.*
import org.scalatest.Inside
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


class UsersSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Inside with DoobieSpec with UsersFixture:
  val initScript           = "sql/users.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Users 'algebra'" - {
    "should retrieve a user by email" in:
      transactor.use: xa =>
        val program =
          for
            users     <- LiveUsers[IO](xa)
            retrieved <- users.find("dawid@dlakomy.github.io")
          yield retrieved

        program.asserting(_ shouldBe Some(dawid))

    "should return None if the email is nonexistent" in:
      transactor.use: xa =>
        val program =
          for
            users     <- LiveUsers[IO](xa)
            retrieved <- users.find("dawidsdfa@lakomy.github.io")
          yield retrieved

        program.asserting(_ shouldBe None)

    "should create a new user" in:
      transactor.use: xa =>
        val program =
          for
            users     <- LiveUsers[IO](xa)
            userId    <- users.create(newUser)
            maybeUser <- sql"SELECT * FROM users WHERE email = ${newUser.email}".query[User].option.transact(xa)
          yield (userId, maybeUser)

        program.asserting: (userId, maybeUser) =>
          userId shouldBe newUser.email
          maybeUser shouldBe Some(newUser)

    "should fail creating a new user if the email already exists" in:
      transactor.use: xa =>
        val program =
          for
            users         <- LiveUsers[IO](xa)
            userIdOrError <- users.create(dawid).attempt
          yield userIdOrError

        program.asserting: outcome =>
          inside(outcome):
            case Left(e) => e shouldBe a[PSQLException]
            case _       => fail()

    "should return None when updating nonexistent user" in:
      transactor.use: xa =>
        val program =
          for
            users     <- LiveUsers[IO](xa)
            maybeUser <- users.update(newUser)
          yield maybeUser

        program.asserting(_ shouldBe None)

    "should update an existing user" in:
      transactor.use: xa =>
        val program =
          for
            users     <- LiveUsers[IO](xa)
            maybeUser <- users.update(updatedJohn)
          yield maybeUser

        program.asserting(_ shouldBe Some(updatedJohn))

    "should delete a user" in:
      transactor.use: xa =>
        val program =
          for
            users     <- LiveUsers[IO](xa)
            result    <- users.delete(dawid.email)
            maybeUser <- sql"SELECT * FROM users WHERE email = ${dawid.email}".query[User].option.transact(xa)
          yield (result, maybeUser)

        program.asserting: (result, maybeUser) =>
          result shouldBe true
          maybeUser shouldBe None

    "should not delete a nonexistent user" in:
      transactor.use: xa =>
        val program =
          for
            users  <- LiveUsers[IO](xa)
            result <- users.delete("nobody@nowhere.com")
          yield result

        program.asserting(_ shouldBe false)

  }
