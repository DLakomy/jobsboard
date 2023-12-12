package dlakomy.jobsboard.pages

import cats.effect.IO
import tyrian.Html.*
import tyrian.*


final case class ForgotPasswordPage() extends Page:
  def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None // TODO

  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) =
    (this, Cmd.None) // TODO

  def view(): Html[Page.Msg] =
    div("Forgot password - TODO")
