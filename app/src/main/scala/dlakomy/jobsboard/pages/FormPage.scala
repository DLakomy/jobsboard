package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.pages.Page.Msg
import org.scalajs.dom.*
import tyrian.Html.*
import tyrian.*

import scala.concurrent.duration.{span as _, *}


abstract class FormPage(title: String, status: Option[Page.Status]) extends Page:
  // abstract API
  protected def renderFormContent(): List[Html[Page.Msg | Router.Msg]]

  // public API
  override def initCmd: Cmd[IO, Msg] =
    clearForm()

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
        id      := formId,
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

  private val formId = "form"
  private def clearForm() =
    // needed cuz the browser kept inserting email and
    // the model didn't know about it
    // possible better solution: use uncontrolled inputs
    // (dunno if possible in Tyrian)
    Cmd.Run[IO, Unit, Page.Msg] {
      def effect: IO[Option[HTMLFormElement]] = for
        maybeForm <- IO(Option(document.getElementById(formId).asInstanceOf[HTMLFormElement]))
        finalForm <-
          if (maybeForm.isEmpty) IO.sleep(100.millis) *> effect
          else IO(maybeForm)
      yield finalForm

      effect.map(_.foreach(_.reset()))
    }(_ => Page.NoOp)
