package dlakomy.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import dlakomy.jobsboard.domain.auth.NewPasswordInfo
import dlakomy.jobsboard.domain.user.*
import dlakomy.jobsboard.fixtures.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt


class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UsersFixture:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockedUsers: Users[IO] = new Users[IO]:
    override def find(email: String): IO[Option[User]] =
      if (email == dawidEmail) IO.pure(Some(dawid))
      else IO.pure(None)
    override def create(user: User): IO[String]       = IO.pure(user.email)
    override def update(user: User): IO[Option[User]] = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean]   = IO.pure(true)

  "Auth 'algebra'" - {
    "login should return None if the users doesn't exist" in:
      val program = for
        auth       <- LiveAuth[IO](mockedUsers)
        maybeToken <- auth.login("nobody@gmail.com", "password")
      yield maybeToken

      program.asserting(_ shouldBe None)

    "login should return None if the users exists, but password is wrong" in:
      val program = for
        auth       <- LiveAuth[IO](mockedUsers)
        maybeToken <- auth.login(dawidEmail, "wrong")
      yield maybeToken

      program.asserting(_ shouldBe None)

    "login should return a token if the users exists, and the password is correct" in:
      val program = for
        auth       <- LiveAuth[IO](mockedUsers)
        maybeToken <- auth.login(dawidEmail, dawidPassword)
      yield maybeToken

      program.asserting(_ shouldBe defined)

    "signing up should not create a user with an existing email" in:
      val program = for
        auth      <- LiveAuth[IO](mockedUsers)
        maybeUser <- auth.signUp(NewUserInfo(dawidEmail, "somepass", None, None, None))
      yield maybeUser

      program.asserting(_ shouldBe None)

    "signing up should create a completely new user" in:
      val newUserInfo = NewUserInfo("bob@gmail.com", "somepass", Some("Bob"), Some("Bobinsky"), Some("Bob S.A."))
      val program = for
        auth      <- LiveAuth[IO](mockedUsers)
        maybeUser <- auth.signUp(newUserInfo)
      yield maybeUser

      program.asserting:
        case Some(user) =>
          user.email shouldBe newUserInfo.email
          user.firstName shouldBe newUserInfo.firstName
          user.lastName shouldBe newUserInfo.lastName
          user.company shouldBe newUserInfo.company
          user.role shouldBe Role.RECRUITER
        case None => fail()

    "change password should return Right(None) if the user doesn't exist" in:
      val program = for
        auth   <- LiveAuth[IO](mockedUsers)
        result <- auth.changePassword("nobody@gmail.com", NewPasswordInfo("password", "newpassword"))
      yield result

      program.asserting(_ shouldBe Right(None))

    "change password should return Left with an error if the password is incorrect" in:
      val program = for
        auth   <- LiveAuth[IO](mockedUsers)
        result <- auth.changePassword(dawidEmail, NewPasswordInfo("invalid", "newpassword"))
      yield result

      program.asserting(_ shouldBe Left("Invalid password"))

    "change password should change password if all params are correct" in:
      val newPassword = "newpassword"
      val program = for
        auth   <- LiveAuth[IO](mockedUsers)
        result <- auth.changePassword(dawidEmail, NewPasswordInfo(dawidPassword, newPassword))
        isNicePassword <- result match
          case Right(Some(user)) =>
            BCrypt.checkpwBool[IO](newPassword, PasswordHash[BCrypt](user.hashedPassword))
          case _ => IO.pure(false)
      yield isNicePassword

      program.asserting(_ shouldBe true)
  }
