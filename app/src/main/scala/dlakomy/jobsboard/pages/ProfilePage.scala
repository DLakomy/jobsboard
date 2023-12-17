package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.core.Session
import dlakomy.jobsboard.domain.auth.NewPasswordInfo
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*


// in the future we could allow company change and other things
final case class ProfilePage(oldPassword: String = "", newPassword: String = "", status: Option[Page.Status] = None)
    extends FormPage("Profile", status):

  import ProfilePage.*

  def update(msg: Page.Msg): (Page, Cmd[IO, Msg]) = msg match
    case UpdateOldPassword(p) =>
      (this.copy(oldPassword = p), Cmd.None)
    case UpdateNewPassword(p) =>
      (this.copy(newPassword = p), Cmd.None)
    case AttemptChangePassword =>
      if (oldPassword.isEmpty)
        (setErrorStatus("Please type your old password."), Cmd.None)
      else if (newPassword.isEmpty)
        (setErrorStatus("Please type a new password."), Cmd.None)
      else
        (this, Commands.changePassword(oldPassword, newPassword))
    case ChangePasswordFailure(error) =>
      (setErrorStatus(error), Cmd.None)
    case ChangePasswordSuccess =>
      (setSuccessStatus("Success! Your password has been changed."), Cmd.None)
    case _ => (this, Cmd.None)

  override protected def renderFormContent(): List[Html[Page.Msg]] = List(
    renderInput("Old password", "oldPassword", "password", true, UpdateOldPassword(_)),
    renderInput("New password", "newPassword", "password", true, UpdateNewPassword(_)),
    button(`type` := "button", onClick(AttemptChangePassword))("Change password")
  )

  override def view() =
    // this logic could be present in the FormPage - we'll see if it's a reusable case
    if (Session.isActive) super.view()
    else renderInvalidPage()
  //////////////////////////////////////////
  // private
  //////////////////////////////////////////
  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  private def renderInvalidPage() =
    div(h1("Profile"), div("You need to be logged in to view this page"))


object ProfilePage:
  trait Msg                                       extends Page.Msg
  case class UpdateOldPassword(email: String)     extends Msg
  case class UpdateNewPassword(email: String)     extends Msg
  case object AttemptChangePassword               extends Msg
  case class ChangePasswordFailure(error: String) extends Msg
  case object ChangePasswordSuccess               extends Msg

  object Endpoints:
    val changePassword = new Endpoint[Msg]:
      override val location: String          = Constants.endpoints.changePassword
      override val method: Method            = Method.Put
      override val onError: HttpError => Msg = e => ChangePasswordFailure(e.toString)
      override val onResponse: Response => Msg = response =>
        response.status match
          case Status(200, _) => ChangePasswordSuccess
          case Status(404, _) => ChangePasswordFailure("The server says that this users doesn't exist :O")
          case Status(s, _) if s >= 400 && s < 500 =>
            val json   = response.body
            val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
            parsed match
              case Left(e)                => ChangePasswordFailure(s"Response error: ${e.getMessage}")
              case Right(errorFromServer) => ChangePasswordFailure(errorFromServer)
          case _ => ChangePasswordFailure("Unknown reply from the server.")

  object Commands:
    def changePassword(oldPassword: String, newPassword: String): Cmd[IO, Msg] =
      Endpoints.changePassword.callAuthorized(NewPasswordInfo(oldPassword, newPassword))
