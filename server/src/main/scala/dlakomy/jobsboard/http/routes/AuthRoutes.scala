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
import tsec.authentication.{Authenticator as _, *}

import scala.language.implicitConversions


class AuthRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (
    auth: Auth[F],
    authenticator: Authenticator[F]
) extends HttpValidationDsl[F]:

  // POST /auth/login { LoginInfo } => 200 OK with JWT as Authorization bearer
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ POST -> Root / "login" =>
      req.withValidated[LoginInfo]: loginInfo =>
        val maybeJwtToken = for
          maybeUser  <- auth.login(loginInfo.email, loginInfo.password)
          maybeToken <- maybeUser.traverse(user => authenticator.create(user.email))
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
            case None       => BadRequest(FailureResponse(s"User with email ${newUserInfo.email} already exists"))
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

  // POST /auth/reset { ForgotPasswordInfo }
  private val forgotPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ POST -> Root / "reset" =>
      for
        forgotPwdInfo <- req.as[ForgotPasswordInfo]
        _             <- auth.sendPasswordRecoveryToken(forgotPwdInfo.email)
        response      <- Ok()
      yield response

  // POST /auth/recover { RecoverPasswordInfo }
  private val recoverPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ POST -> Root / "recover" =>
      for
        rpInfo             <- req.as[RecoverPasswordInfo]
        recoverySuccessful <- auth.recoverPasswordFromToken(rpInfo.email, rpInfo.token, rpInfo.newPassword)
        resp <- if (recoverySuccessful) Ok() else Forbidden(FailureResponse("Email/token combination is incorrect"))
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
    case req @ DELETE -> Root / "users" / email asAuthed _ =>
      auth
        .delete(email)
        .flatMap:
          case true  => Ok()
          case false => NotFound()

  private val checkTokenRoute: AuthRoute[F] =
    case GET -> Root / "checkToken" asAuthed _ =>
      Ok()

  private val unauthedRoutes =
    loginRoute <+> createUserRoute <+> forgotPasswordRoute <+> recoverPasswordRoute

  private val authedRoutes = SecuredHandler[F].liftService(
    changePasswordRoute.restrictedTo(allRoles) |+|
      logoutRoute.restrictedTo(allRoles) |+|
      deleteUserRoute.restrictedTo(adminOnly) |+|
      checkTokenRoute.restrictedTo(allRoles)
  )

  val routes = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )


object AuthRoutes:
  def apply[F[_]: Concurrent: Logger: SecuredHandler](
      auth: Auth[F],
      authenticator: Authenticator[F]
  ) =
    new AuthRoutes[F](auth, authenticator)
