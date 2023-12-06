package dlakomy.jobsboard.http.routes

import cats.effect.*
import cats.syntax.all.*
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.domain.auth.*
import dlakomy.jobsboard.domain.security.*
import dlakomy.jobsboard.domain.user.*
import dlakomy.jobsboard.http.responses.*
import dlakomy.jobsboard.http.validation.syntax.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger
import tsec.authentication.*

import scala.language.implicitConversions


class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F]:

  private val authenticator = auth.authenticator

  private val securedHandler: SecuredRequestHandler[F, String, User, JwtToken] = SecuredRequestHandler(authenticator)

  // POST /auth/login { LoginInfo } => 200 OK with JWT as Authorization bearer
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ POST -> Root / "login" =>
      req.withValidated[LoginInfo]: loginInfo =>
        val maybeJwtToken = for
          maybeToken <- auth.login(loginInfo.email, loginInfo.password)
          _          <- Logger[F].info(s"User logging in: ${loginInfo.email}")
        yield maybeToken

        maybeJwtToken.map:
          case Some(token) => authenticator.embed(Response(Status.Ok), token)
          case None        => Response(Status.Unauthorized)

  // POST /auth/users { NewUserInfo } => 201 Created | BadRequest (eg. when user exists)
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ POST -> Root / "users" =>
      req.withValidated[NewUserInfo]: newUserInfo =>
        for
          maybeNewUser <- auth.signUp(newUserInfo)
          resp <- maybeNewUser match
            case Some(user) => Created(user.email)
            case None       => BadRequest(s"User with email ${newUserInfo.email} already exists")
        yield resp

  // PUT /auth/users/password { NewPasswordInfo } { Authorization: Bearer {jwt} } => 200 OK
  private val changePasswordRoute: AuthRoute[F] =
    case req @ PUT -> Root / "users" / "password" asAuthed user =>
      req.request.withValidated[NewPasswordInfo]: newPasswordInfo =>
        for
          maybeUserOrError <- auth.changePassword(user.email, newPasswordInfo)
          resp <- maybeUserOrError match
            case Right(Some(_)) => Ok()
            case Right(None)    => NotFound(FailureResponse(s"User ${user.email} not found"))
            case Left(_)        => Forbidden()
        yield resp

  // POST /auth/logout { Authorization: Bearer {jwt} } => 200 OK
  private val logoutRoute: AuthRoute[F] =
    case req @ POST -> Root / "logout" asAuthed _ =>
      val token = req.authenticator
      for
        _    <- authenticator.discard(token)
        resp <- Ok()
      yield resp

  // DELETE /auth/users/{email}
  private val deleteUserRoute: AuthRoute[F] =
    case req @ delete -> Root / "users" / email asAuthed _ =>
      auth
        .delete(email)
        .flatMap:
          case true  => Ok()
          case false => NotFound()

  private val unauthedRoutes = loginRoute <+> createUserRoute
  private val authedRoutes = securedHandler.liftService(
    changePasswordRoute.restrictedTo(allRoles) |+|
      logoutRoute.restrictedTo(allRoles) |+|
      deleteUserRoute.restrictedTo(adminOnly)
  )

  val routes = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )


object AuthRoutes:
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]) =
    new AuthRoutes[F](auth)
