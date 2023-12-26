package dlakomy.jobsboard

import cats.effect.IO
import dlakomy.jobsboard.components.*
import dlakomy.jobsboard.pages.*
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.document
import org.scalajs.dom.window
import tyrian.Html.*
import tyrian.*

import scala.scalajs.js.annotation.*

import core.*


object App:
  // Daniel did a trait App.Msg with these members
  // TBH his solution appears to be easier to use
  // the distinction looks noice, but is a hassle
  // to maintain
  type Msg = Router.Msg | Session.Msg | Page.Msg

  case class Model(router: Router, session: Session, page: Page)


@JSExportTopLevel("JobsboardApp")
class App extends TyrianApp[App.Msg, App.Model]:
  import App.*

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    val location            = window.location.pathname
    val page                = Page.get(location)
    val pageCmd             = page.initCmd
    val (router, routerCmd) = Router.startAt(location)
    val session             = Session()
    val sessionCmd          = session.initCmd
    (Model(router, session, page), routerCmd |+| sessionCmd |+| pageCmd)

  override def subscriptions(model: Model): Sub[IO, Msg] =
    val urlChange = Sub.make( // listener for browser history changes (like back btn)
      "urlChange",
      model.router.history.state.discrete
        .map(_.get)
        .map(newLocation => Router.ChangeLocation(newLocation, true))
    )

    val enterPress = Sub.fromEvent[IO, KeyboardEvent, Msg]("keyup", document):
      case e: KeyboardEvent =>
        Option.when(e.key == "Enter")(Page.EnterPressed)

    urlChange |+| enterPress

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case msg: Router.Msg =>
      val (newRouter, routerCmd) = model.router.update(msg)
      if (model.router == newRouter) // no change necessary
        (model, Cmd.None)
      else // location change, rerendering needed
        val newPage    = Page.get(newRouter.location)
        val newPageCmd = newPage.initCmd
        (model.copy(router = newRouter, page = newPage), routerCmd |+| newPageCmd)
    case msg: Session.Msg =>
      val (newSession, cmd) = model.session.update(msg)
      (model.copy(session = newSession), cmd)
    case msg: Page.Msg =>
      val (newPage, cmd) = model.page.update(msg)
      (model.copy(page = newPage), cmd)

  override def view(model: Model): Html[Msg] =
    div(
      Header.view(),
      main(
        div(`class` := "container-fluid")(model.page.view())
      )
    )
