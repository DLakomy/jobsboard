package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.pages.Page.Msg
import tyrian.Html.*
import tyrian.*


abstract class FormPage(title: String, status: Option[Page.Status]) extends Page:
  // abstract API
  protected def renderFormContent(): List[Html[Page.Msg | Router.Msg]]

  // public API
  override def initCmd: Cmd[IO, Msg] = Cmd.None

  final override def view(): Html[Page.Msg | Router.Msg] = renderForm()

  // protected API
  final protected def renderForm(): Html[Page.Msg | Router.Msg] =
    div(`class` := "form-section")(
      div(`class` := "top-section")(
        h1(title)
      ),
      form(
        name    := title.toLowerCase.replace(' ', '_'),
        `class` := "form",
        onEvent(
          "submit",
          e =>
            e.preventDefault()
            Page.NoOp
        )
      )(
        renderFormContent()
      ),
      status.map(s => div(s.message)).getOrElse(div())
    )

  final protected def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      onChange: String => Msg
  ) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  final protected def renderAuxLink(location: String, text: String): Html[Router.Msg] =
    a(
      href    := location,
      `class` := "aux-link",
      onEvent(
        "click",
        e =>
          e.preventDefault()
          Router.ChangeLocation(location)
      )
    )(text)
