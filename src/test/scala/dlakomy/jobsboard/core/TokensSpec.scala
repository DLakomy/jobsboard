package dlakomy.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import dlakomy.jobsboard.config.TokenConfig
import dlakomy.jobsboard.domain.job.JobFilter
import dlakomy.jobsboard.domain.pagination.Pagination
import dlakomy.jobsboard.fixtures.JobFixture
import dlakomy.jobsboard.fixtures.UsersFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*


class TokensSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with UsersFixture:

  val initScript           = "sql/recoverytokens.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Tokens 'algebra'" - {
    "should not create a new token for a non-existing user" in:
      transactor.use: xa =>
        val program = for
          tokens     <- LiveTokens(mockedUsers)(xa, TokenConfig(10000000L))
          maybeToken <- tokens.getToken("nobody@never.com")
        yield maybeToken

        program.asserting(_ shouldBe None)

    "should create a new token for an existing user" in:
      transactor.use: xa =>
        val program = for
          tokens     <- LiveTokens(mockedUsers)(xa, TokenConfig(10000000L))
          maybeToken <- tokens.getToken(dawidEmail)
        yield maybeToken

        program.asserting(_ shouldBe defined)

    "should correctly detect expired tokens" in:
      transactor.use: xa =>
        val program = for
          tokens     <- LiveTokens(mockedUsers)(xa, TokenConfig(10L))
          maybeToken <- tokens.getToken(dawidEmail)
          _          <- IO.sleep(100.millis)
          isTokenValid <-
            maybeToken.fold(IO.pure(false))(token => tokens.checkToken(dawidEmail, token))
        yield isTokenValid

        program.asserting(_ shouldBe false)

    "should validate unexpired tokens" in:
      transactor.use: xa =>
        val program = for
          tokens     <- LiveTokens(mockedUsers)(xa, TokenConfig(10000000L))
          maybeToken <- tokens.getToken(dawidEmail)
          isTokenValid <-
            maybeToken.fold(IO.pure(false))(token => tokens.checkToken(dawidEmail, token))
        yield isTokenValid

        program.asserting(_ shouldBe true)

    "should only validate tokens only for the user that generated them" in:
      transactor.use: xa =>
        val program = for
          tokens     <- LiveTokens(mockedUsers)(xa, TokenConfig(10000000L))
          maybeToken <- tokens.getToken(dawidEmail)
          isDawidTokenValid <-
            maybeToken.fold(IO.pure(false))(token => tokens.checkToken(dawidEmail, token))
          isOtherTokenValid <-
            maybeToken.fold(IO.pure(false))(token => tokens.checkToken("someone@else.com", token))
        yield (isDawidTokenValid, isOtherTokenValid)

        program.asserting: (isDawidTokenValid, isOtherTokenValid) =>
          isDawidTokenValid shouldBe true
          isOtherTokenValid shouldBe false

  }
