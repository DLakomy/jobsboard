package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.domain.auth.*
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*


final case class SignUpPage(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    firstName: String = "",
    lastName: String = "",
    company: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Sign up", status):
  import SignUpPage.*

  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case UpdateEmail(e)            => (this.copy(email = e), Cmd.None)
    case UpdatePassword(p)         => (this.copy(password = p), Cmd.None)
    case UpdateConfirmPassword(cp) => (this.copy(confirmPassword = cp), Cmd.None)
    case UpdateFirstName(fn)       => (this.copy(firstName = fn), Cmd.None)
    case UpdateLastName(ln)        => (this.copy(lastName = ln), Cmd.None)
    case UpdateCompany(c)          => (this.copy(company = c), Cmd.None)
    case AttemptSignUp =>
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Email is invalid"), Cmd.None)
      else if (password.isEmpty)
        (setErrorStatus("Please enter a password"), Cmd.None)
      else if (password != confirmPassword)
        (setErrorStatus("Password fields do not match"), Cmd.None)
      else
        (
          this,
          Commands.signUp(
            NewUserInfo(
              email,
              password,
              Option(firstName).filter(_.nonEmpty),
              Option(lastName).filter(_.nonEmpty),
              Option(company).filter(_.nonEmpty)
            )
          )
        )
    case SignUpError(message) =>
      (setErrorStatus(message), Cmd.None)
    case SignUpSuccess(message) =>
      (setSuccessStatus(message), Cmd.None)
    case _ => (this, Cmd.None)

  override protected def renderFormContent(): List[Html[Page.Msg]] =
    List(
      renderInput("Email", "email", "text", true, UpdateEmail(_)),
      renderInput("Password", "password", "password", true, UpdatePassword(_)),
      renderInput("Confirm password", "cPassword", "password", true, UpdateConfirmPassword(_)),
      renderInput("First name", "firstName", "text", false, UpdateFirstName(_)),
      renderInput("Last name", "lasttName", "text", false, UpdateLastName(_)),
      renderInput("Company", "company", "text", false, UpdateCompany(_)),
      button(`type` := "button", onClick(AttemptSignUp))("Sign up")
    )
  //////////////////////////////////////////
  // private
  //////////////////////////////////////////
  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))


object SignUpPage:
  trait Msg                                                 extends Page.Msg
  case class UpdateEmail(email: String)                     extends Msg
  case class UpdatePassword(password: String)               extends Msg
  case class UpdateConfirmPassword(confirmPassword: String) extends Msg
  case class UpdateFirstName(firstName: String)             extends Msg
  case class UpdateLastName(lastName: String)               extends Msg
  case class UpdateCompany(company: String)                 extends Msg
  // actions
  case object AttemptSignUp extends Msg
  case object NoOp          extends Msg
  // statuses
  case class SignUpError(message: String)   extends Msg
  case class SignUpSuccess(message: String) extends Msg

  object Endpoints:
    val signUp = new Endpoint[Msg]:
      override val location = Constants.endpoints.signUp
      override val method   = Method.Post
      override val onResponse: Response => Msg = response =>
        response.status match
          case Status(201, _) =>
            SignUpSuccess("Success! Log in now")
          case Status(s, _) if s >= 400 && s < 500 =>
            val json   = response.body
            val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
            parsed match
              case Left(e)  => SignUpError(s"Error: ${e.getMessage}")
              case Right(e) => SignUpError(e)

      override val onError: HttpError => Msg =
        e => SignUpError(e.toString)

  object Commands:
    def signUp(newUserInfo: NewUserInfo): Cmd[IO, Msg] =
      Endpoints.signUp.call(newUserInfo)
