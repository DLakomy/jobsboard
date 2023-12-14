package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.Constants
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.domain.auth.LoginInfo
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.App


final case class LoginPage(email: String = "", password: String = "", status: Option[Page.Status] = None) extends Page:
  import LoginPage.*

  def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None

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
      (setSuccessStatus("Success!"), Cmd.Emit(Session.SetToken(email, token)))
    case _ => (this, Cmd.None)

  def view(): Html[Page.Msg] =
    div(`class` := "form-section")(
      div(`class` := "top-section")(
        h1("Sign up")
      ),
      form(
        name    := "signin",
        `class` := "form",
        onEvent(
          "submit",
          e =>
            e.preventDefault()
            NoOp
        )
      )(
        renderInput("Email", "email", "text", true, UpdateEmail(_)),
        renderInput("Password", "password", "password", true, UpdatePassword(_)),
        button(`type` := "button", onClick(AttemptLogin))("Sign up")
      ),
      status.map(s => div(s.message)).getOrElse(div())
    )

  //////////////////////////////////////////
  // private
  //////////////////////////////////////////
  private def renderInput(name: String, uid: String, kind: String, isRequired: Boolean, onChange: String => Msg) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

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
      override val location = Constants.Endpoints.login
      override val method   = Method.Post
      override val onSuccess: Response => Msg = response =>
        val maybeToken = response.headers.get("authorization")
        maybeToken match
          case Some(token) => LoginSuccess(token)
          case None        => LoginError("Invalid username or password")

      override val onError: HttpError => Msg =
        e => LoginError(e.toString)

  object Commands:
    def login(loginInfo: LoginInfo): Cmd[IO, Msg] =
      Endpoints.login.call(loginInfo)
