package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.domain.auth.ForgotPasswordInfo
import dlakomy.jobsboard.pages.Page.Msg
import io.circe.generic.auto.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.HttpError
import tyrian.http.Method
import tyrian.http.Response


final case class ForgotPasswordPage(email: String = "", status: Option[Page.Status] = None)
    extends FormPage("Reset password", status):

  import ForgotPasswordPage.*

  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case UpdateEmail(e) =>
      (this.copy(email = e), Cmd.None)
    case AttemptResetPassword =>
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Please insert a valid email."), Cmd.None)
      else
        (this, Commands.resetPassword(email))
    case ResetSuccess =>
      (setSuccessStatus("Check your email!"), Cmd.None)
    case ResetFailure(error) =>
      (setErrorStatus(error), Cmd.None)
    case _ => (this, Cmd.None)

  override protected def renderFormContent(): List[Html[Page.Msg]] =
    List(
      renderInput("Email", "email", "text", true, UpdateEmail(_)),
      button(`type` := "button", onClick(AttemptResetPassword))("Send email")
    )

  //////////////////////////////////////////
  // private
  //////////////////////////////////////////
  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))


object ForgotPasswordPage:
  trait Msg                              extends Page.Msg
  case class UpdateEmail(email: String)  extends Msg
  case object AttemptResetPassword       extends Msg
  case class ResetFailure(error: String) extends Msg
  case object ResetSuccess               extends Msg

  object Endpoints:
    val resetPassword = new Endpoint[Msg]:
      override val location: String            = Constants.endpoints.forgotPassword
      override val method: Method              = Method.Post
      override val onError: HttpError => Msg   = e => ResetFailure(e.toString)
      override val onResponse: Response => Msg = _ => ResetSuccess

  object Commands:
    def resetPassword(email: String) =
      Endpoints.resetPassword.call(ForgotPasswordInfo(email))
