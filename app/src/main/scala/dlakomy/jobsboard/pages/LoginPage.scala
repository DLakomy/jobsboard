package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.App
import dlakomy.jobsboard.common.Constants
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.domain.auth.LoginInfo
import io.circe.generic.auto.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*


final case class LoginPage(email: String = "", password: String = "", status: Option[Page.Status] = None)
    extends FormPage("Log in", status):
  import LoginPage.*

  def update(msg: Page.Msg): (Page, Cmd[IO, App.Msg]) = msg match
    case UpdateEmail(e)    => (this.copy(email = e), Cmd.None)
    case UpdatePassword(p) => (this.copy(password = p), Cmd.None)
    case AttemptLogin =>
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Email is invalid"), Cmd.None)
      else if (password.isEmpty)
        (setErrorStatus("Please enter a password"), Cmd.None)
      else
        (
          this,
          Commands.login(
            LoginInfo(
              email,
              password
            )
          )
        )
    case LoginError(error) =>
      (setErrorStatus(error), Cmd.None)
    case LoginSuccess(token) =>
      (setSuccessStatus("Success!"), Cmd.Emit(Session.SetToken(email, token, true)))
    case _ => (this, Cmd.None)

  override protected def renderFormContent(): List[Html[Page.Msg | Router.Msg]] =
    List(
      renderInput("Email", "email", "text", true, UpdateEmail(_)),
      renderInput("Password", "password", "password", true, UpdatePassword(_)),
      button(`type` := "button", onClick(AttemptLogin))("Sign up"),
      renderAuxLink(Page.Urls.FORGOT_PASSWORD, "Forgot password?")
    )
  //////////////////////////////////////////
  // private
  //////////////////////////////////////////
  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))


object LoginPage:
  trait Msg                                   extends Page.Msg
  case class UpdateEmail(email: String)       extends Msg
  case class UpdatePassword(password: String) extends Msg
  // actions
  case object AttemptLogin extends Msg
  case object NoOp         extends Msg
  // statuses
  case class LoginError(message: String) extends Msg
  case class LoginSuccess(token: String) extends Msg

  object Endpoints:
    val login = new Endpoint[Msg]:
      override val location = Constants.endpoints.login
      override val method   = Method.Post
      override val onResponse: Response => Msg = response =>
        val maybeToken = response.headers.get("authorization")
        maybeToken match
          case Some(token) => LoginSuccess(token)
          case None        => LoginError("Invalid username or password")

      override val onError: HttpError => Msg =
        e => LoginError(e.toString)

  object Commands:
    def login(loginInfo: LoginInfo): Cmd[IO, Msg] =
      Endpoints.login.call(loginInfo)
