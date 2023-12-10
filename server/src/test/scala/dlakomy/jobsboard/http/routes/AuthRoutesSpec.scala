package dlakomy.jobsboard.http.routes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.*
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.domain.auth.*
import dlakomy.jobsboard.domain.user.*
import dlakomy.jobsboard.fixtures.*
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


class AuthRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with UsersFixture
    with SecuredRouteFixture:

  ////////////////////////////////////////////////////////////
  // PREP
  ////////////////////////////////////////////////////////////
  private val mockedAuth: Auth[IO] = probedAuth(None)

  private def probedAuth(userMap: Option[Ref[IO, Map[String, String]]]): Auth[IO] = new Auth[IO]:
    override def login(email: String, password: String): IO[Option[User]] =
      if (email == dawidEmail && password == dawidPassword)
        IO.pure(Some(dawid))
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

    override def sendPasswordRecoveryToken(email: String): IO[Unit] =
      userMap
        .traverse: userMapRef =>
          userMapRef.modify: userMap =>
            (userMap + (email -> "aqq123"), ())
        .map(_ => ())

    override def recoverPasswordFromToken(email: String, token: String, newPassword: String): IO[Boolean] =
      userMap // functional programming is readable, they said...
        .traverse: userMapRef =>
          userMapRef.get
            .map: userMap =>
              userMap.get(email).filter(_ == token)
            .map(_.nonEmpty)
        .map(_.getOrElse(false))

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  // this is what we are testing
  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth, mockedAuthenticator).routes

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
      for response <- authRoutes.orNotFound.run(
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
          Request(method = Method.DELETE, uri = uri"/auth/users/dawid@dlakomy.github.io")
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

    "should return 200 when resetting a password and an email should be triggered" in:
      for
        userMapRef <- Ref.of[IO, Map[String, String]](Map.empty)
        auth       <- IO.pure(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/reset")
            .withEntity(ForgotPasswordInfo(dawidEmail))
        )
        userMap <- userMapRef.get
      yield
        response.status shouldBe Status.Ok
        userMap should contain key dawidEmail

    "should return 200 when recovering a password with valid user+token" in:
      for
        userMapRef <- Ref.of[IO, Map[String, String]](Map(dawidEmail -> "aqq123"))
        auth       <- IO.pure(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/recover")
            .withEntity(RecoverPasswordInfo(dawidEmail, "aqq123", "newpass"))
        )
      yield response.status shouldBe Status.Ok

    "should return 403 when recovering a password with invalid token or no token" in:
      for
        userMapRef <- Ref.of[IO, Map[String, String]](Map(dawidEmail -> "aqq123"))
        auth       <- IO.pure(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/recover")
            .withEntity(RecoverPasswordInfo(dawidEmail, "wrong", "newpass"))
        )
      yield response.status shouldBe Status.Forbidden

  }
