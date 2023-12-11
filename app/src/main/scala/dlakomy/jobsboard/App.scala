package dlakomy.jobsboard

import cats.effect.IO
import org.scalajs.dom.window
import tyrian.Html.*
import tyrian.*

import scala.scalajs.js.annotation.*

import core.*


object App:
  type Msg = Router.Msg

  final case class Model(router: Router)


@JSExportTopLevel("JobsboardApp")
class App extends TyrianApp[App.Msg, App.Model]:
  import App.*

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    val (router, cmd) = Router.startAt(window.location.pathname)
    (Model(router), cmd)

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.make( // listener for browser history changes (like back btn)
      "urlChange",
      model.router.history.state.discrete
        .map(_.get)
        .map(newLocation => Router.ChangeLocation(newLocation, true))
    )

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case msg: Router.Msg =>
      val (newRouter, cmd) = model.router.update(msg)
      (model.copy(router = newRouter), cmd)

  override def view(model: Model): Html[Msg] =
    div(
      renderNavLink("Jobs", "/jobs"),
      renderNavLink("Login", "/login"),
      renderNavLink("Sign up", "/signup"),
      div(s"You are now at: ${model.router.location}")
    )

  private def renderNavLink(text: String, location: String) =
    a(
      href    := location,
      `class` := "nav-link",
      onEvent(
        "click",
        e =>
          e.preventDefault()
          Router.ChangeLocation(location)
      )
    )(text)
