package dlakomy.jobsboard.pages

import cats.effect.IO
import tyrian.*


object Page:
  trait Msg

  object Urls:
    val LOGIN            = "/login"
    val SIGNUP           = "/signup"
    val FORGOT_PASSWORD  = "/forgotpassword"
    val RECOVER_PASSWORD = "recoverpassword"
    val JOBS             = "/jobs"
    val EMPTY            = ""
    val HOME             = "/"

  def get(location: String) =
    import Urls.*
    location match
      case LOGIN               => LoginPage()
      case SIGNUP              => SignUpPage()
      case FORGOT_PASSWORD     => ForgotPasswordPage()
      case RECOVER_PASSWORD    => RecoverPasswordPage()
      case EMPTY | HOME | JOBS => JobListPage()
      case s"/jobs/$id"        => JobPage(id)
      case _                   => NotFoundPage()


abstract class Page:
  def initCmd: Cmd[IO, Page.Msg]
  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg])
  def view(): Html[Page.Msg]