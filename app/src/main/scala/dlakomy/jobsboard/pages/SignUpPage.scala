package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.*
import tyrian.Html.*
import tyrian.*
import tyrian.cmds.Logger


final case class SignUpPage(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    firstName: String = "",
    lastName: String = "",
    company: String = "",
    status: Option[Page.Status] = None
) extends Page:
  import SignUpPage.*

  def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None

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
      else (this, Logger.consoleLog[IO]("Signing up!", email, password, firstName, lastName, company)) // TODO
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
        renderInput("Confirm password", "cPassword", "password", true, UpdateConfirmPassword(_)),
        renderInput("First name", "firstName", "text", false, UpdateFirstName(_)),
        renderInput("Last name", "lasttName", "text", false, UpdateLastName(_)),
        renderInput("Company", "company", "text", false, UpdateCompany(_)),
        button(`type` := "button", onClick(AttemptSignUp))("Sign up")
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
