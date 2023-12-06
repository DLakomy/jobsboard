package dlakomy.jobsboard.http.routes

import cats.data.OptionT
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.domain.auth.*
import dlakomy.jobsboard.domain.security.*
import dlakomy.jobsboard.domain.user.*
import dlakomy.jobsboard.fixtures.*
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import concurrent.duration.*


class AuthRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Http4sDsl[IO] with UsersFixture:

  ////////////////////////////////////////////////////////////
  // PREP
  ////////////////////////////////////////////////////////////
  private val mockedAuth: Auth[IO] = new Auth[IO]:
    override def login(email: String, password: String): IO[Option[JwtToken]] =
      if (email == dawidEmail && password == dawidPassword)
        mockedAuthenticator.create(dawidEmail).map(Some(_))
      else
        IO.pure(None)

    override def signUp(newUserInfo: NewUserInfo): IO[Option[User]] =
      if (newUserInfo.email == johnEmail)
        IO.pure(Some(john))
      else
        IO.pure(None)

    override def changePassword(email: String, newPasswordInfo: NewPasswordInfo): IO[Either[String, Option[User]]] =
      if (email == dawidEmail)
        if (newPasswordInfo.oldPassword == dawidPassword)
          IO.pure(Right(Some(dawid)))
        else
          IO.pure(Left("Invalid password"))
      else
        IO.pure(Right(None))

    override def delete(email: String): IO[Boolean] = IO.pure(true)

    override def authenticator: Authenticator[IO] = mockedAuthenticator

  private val mockedAuthenticator: Authenticator[IO] =
    val key =
      HMACSHA256.unsafeGenerateKey
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == dawidEmail) OptionT.pure(dawid)
      else if (email == johnEmail) OptionT.pure(john)
      else OptionT.none

    JWTAuthenticator.unbacked.inBearerToken(1.day, None, idStore, key)

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  // this is what we are testing
  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth).routes

  extension (r: Request[IO])
    def withBearerToken(a: JwtToken): Request[IO] =
      r.putHeaders:
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))

  ////////////////////////////////////////////////////////////
  // TESTS
  ////////////////////////////////////////////////////////////
  "AuthRoutes" - {
    "should return a 401 - unauthorized if login fails" in:
      for response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login").withEntity(LoginInfo(dawidEmail, "wrongpass"))
        )
      yield response.status shouldBe Status.Unauthorized

    "should return a 200 + jwt if login successful" in:
      for response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login").withEntity(LoginInfo(dawidEmail, dawidPassword))
        )
      yield
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined

    "should return a 400 if the user to create already in db" in:
      for response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users").withEntity(newUserDawid)
        )
      yield response.status shouldBe Status.BadRequest

    "should return 201 if the user is created" in:
      for response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users").withEntity(newUserJohn)
        )
      yield response.status shouldBe Status.Created

    "should return 401 if logout has no jwt" in:
      for
        jwtToken <- mockedAuthenticator.create(dawidEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      yield response.status shouldBe Status.Unauthorized

    "should return 200 if logout with valid jwt" in:
      for
        jwtToken <- mockedAuthenticator.create(dawidEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout").withBearerToken(jwtToken)
        )
      yield response.status shouldBe Status.Ok

    "should return 404 if changing pass for nonexistent user" in:
      for
        jwtToken <- mockedAuthenticator.create(johnEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(johnPassword, "newpass"))
        )
      yield response.status shouldBe Status.NotFound

    "should return 403 if changing pass with invalid old pass" in:
      for
        jwtToken <- mockedAuthenticator.create(dawidEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("invalid", "newpass"))
        )
      yield response.status shouldBe Status.Forbidden

    "should return 401 if changing pass with no valid JWT" in:
      for response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(dawidPassword, "newpass"))
        )
      yield response.status shouldBe Status.Unauthorized

    "should return 200 when changing password successfully" in:
      for
        jwtToken <- mockedAuthenticator.create(dawidEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(dawidPassword, "newpass"))
        )
      yield response.status shouldBe Status.Ok

    "should return 401 if a non-admin tries to delete a user" in:
      for
        jwtToken <- mockedAuthenticator.create(johnEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/dawid@lakomy.github.io")
            .withBearerToken(jwtToken)
        )
      yield response.status shouldBe Status.Unauthorized

    "should return 200 if an admin tries to delete a user" in:
      for
        jwtToken <- mockedAuthenticator.create(dawidEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/john@lakomy.github.io")
            .withBearerToken(jwtToken)
        )
      yield response.status shouldBe Status.Ok

  }
