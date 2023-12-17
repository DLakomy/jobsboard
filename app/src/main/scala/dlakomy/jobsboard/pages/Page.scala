package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.App
import dlakomy.jobsboard.core.Router
import tyrian.*


object Page:
  trait Msg
  case object NoOp extends Msg

  enum StatusKind:
    case SUCCESS, ERROR, LOADING

  final case class Status(message: String, kind: StatusKind)

  object urls:
    val LOGIN           = "/login"
    val SIGNUP          = "/signup"
    val FORGOT_PASSWORD = "/forgotpassword"
    val RESET_PASSWORD  = "/resetpassword"
    val JOBS            = "/jobs"
    val EMPTY           = ""
    val HOME            = "/"
    val HASH            = "#"
    val PROFILE         = "/profile"

  def get(location: String) =
    import urls.*
    location match
      case LOGIN               => LoginPage()
      case SIGNUP              => SignUpPage()
      case FORGOT_PASSWORD     => ForgotPasswordPage()
      case RESET_PASSWORD      => ResetPasswordPage()
      case EMPTY | HOME | JOBS => JobListPage()
      case s"/jobs/$id"        => JobPage(id)
      case PROFILE             => ProfilePage()
      case _                   => NotFoundPage()


abstract class Page:
  def initCmd: Cmd[IO, Page.Msg]
  def update(msg: Page.Msg): (Page, Cmd[IO, App.Msg])
  def view(): Html[Page.Msg | Router.Msg]
