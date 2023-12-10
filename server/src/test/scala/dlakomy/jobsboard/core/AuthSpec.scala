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

  private val validToken = "aqq123"
  private val mockedTokens: Tokens[IO] = new Tokens[IO]:

    override def getToken(email: String): IO[Option[String]] =
      if (email == dawidEmail) IO.pure(Some(validToken))
      else IO.pure(None)

    override def checkToken(email: String, token: String): IO[Boolean] =
      IO.pure(token == validToken)

  private val mockedEmails: Emails[IO] = new Emails[IO]:
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] = IO.unit
    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit]    = IO.unit

  private def probedEmails(users: Ref[IO, Set[String]]): Emails[IO] = new Emails[IO]:
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] =
      users.modify(set => (set + to, ()))
    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] =
      sendEmail(to, "your token", "token")

  "Auth 'algebra'" - {
    "login should return None if the users doesn't exist" in:
      val program = for
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login("nobody@gmail.com", "password")
      yield maybeToken

      program.asserting(_ shouldBe None)

    "login should return None if the users exists, but password is wrong" in:
      val program = for
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(dawidEmail, "wrong")
      yield maybeToken

      program.asserting(_ shouldBe None)

    "login should return a token if the users exists, and the password is correct" in:
      val program = for
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(dawidEmail, dawidPassword)
      yield maybeToken

      program.asserting(_ shouldBe defined)

    "signing up should not create a user with an existing email" in:
      val program = for
        auth      <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeUser <- auth.signUp(NewUserInfo(dawidEmail, "somepass", None, None, None))
      yield maybeUser

      program.asserting(_ shouldBe None)

    "signing up should create a completely new user" in:
      val newUserInfo = NewUserInfo("bob@gmail.com", "somepass", Some("Bob"), Some("Bobinsky"), Some("Bob S.A."))
      val program = for
        auth      <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
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
        auth   <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword("nobody@gmail.com", NewPasswordInfo("password", "newpassword"))
      yield result

      program.asserting(_ shouldBe Right(None))

    "change password should return Left with an error if the password is incorrect" in:
      val program = for
        auth   <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(dawidEmail, NewPasswordInfo("invalid", "newpassword"))
      yield result

      program.asserting(_ shouldBe Left("Invalid password"))

    "change password should change password if all params are correct" in:
      val newPassword = "newpassword"
      val program = for
        auth   <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(dawidEmail, NewPasswordInfo(dawidPassword, newPassword))
        isNicePassword <- result match
          case Right(Some(user)) =>
            BCrypt.checkpwBool[IO](newPassword, PasswordHash[BCrypt](user.hashedPassword))
          case _ => IO.pure(false)
      yield isNicePassword

      program.asserting(_ shouldBe true)

    "recoverPassword should fail for a user that does not exist, even if the token is correct" in:
      val program = for
        auth    <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result1 <- auth.recoverPasswordFromToken("nobody@gmail.com", validToken, "whatever")
        result2 <- auth.recoverPasswordFromToken("nobody@gmail.com", "fdsaf", "whatever")
      yield (result1, result2)

      program.asserting(_ shouldBe (false, false))

    "recoverPassword should fail for a user that does exist, but the token is wrong" in:
      val program = for
        auth   <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(dawidEmail, "dsgasdg", "h4ck3d")
      yield result

      program.asserting(_ shouldBe false)

    "recoverPassword should succeed for a correct user+token combination" in:
      val program = for
        auth   <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(dawidEmail, validToken, "newpass")
      yield result

      program.asserting(_ shouldBe true)

    "sending recovery tokens should fail for a user that doesn't exist" in:
      val program = for
        set        <- Ref.of[IO, Set[String]](Set.empty)
        emails     <- IO(probedEmails(set))
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        result     <- auth.sendPasswordRecoveryToken("nobody@whatevere.com")
        sentEmails <- set.get
      yield sentEmails

      program.asserting(_ shouldBe empty)

    "sending recovery tokens should succeed for a user that exists" in:
      val program = for
        set        <- Ref.of[IO, Set[String]](Set.empty)
        emails     <- IO(probedEmails(set))
        auth       <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        result     <- auth.sendPasswordRecoveryToken(dawidEmail)
        sentEmails <- set.get
      yield sentEmails

      program.asserting(_ should contain(dawidEmail))

  }
