package dlakomy.jobsboard.pages

import cats.effect.IO
import tyrian.Html.*
import tyrian.*


final case class NotFoundPage() extends Page:
  def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None

  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) =
    (this, Cmd.None)

  def view(): Html[Page.Msg] =
    div("Not found page - TODO")
