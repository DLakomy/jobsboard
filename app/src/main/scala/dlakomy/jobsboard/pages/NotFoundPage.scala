package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.Constants
import tyrian.Html.*
import tyrian.*


final case class NotFoundPage() extends Page:
  def initCmd: Cmd[IO, Page.Msg] =
    Cmd.None

  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) =
    (this, Cmd.None)

  def view(): Html[Page.Msg] =
    div(`class` := "row")(
      div(`class` := "col-md-5 p-0")(
        div(`class` := "logo")(
          img(src := Constants.logoImage)
        )
      ),
      div(`class` := "col-md-7")(
        div(`class` := "form-section")(
          div(`class` := "top-section")(
            h1(span("Ouch, page not found")),
            div("This page doesn't exist.")
          )
        )
      )
    )
