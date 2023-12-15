package dlakomy.jobsboard.components

import cats.effect.IO
import dlakomy.jobsboard.App
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.pages.*
import tyrian.Html.*
import tyrian.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*


object Header:

  def view() =
    div(`class` := "header-container")(
      renderLogo(),
      div(`class` := "header-nav")(
        ul(`class` := "header-links")(
          renderNavLinks()
        )
      )
    )

  /////////////////////////// private
  @js.native
  @JSImport("/static/img/rtjvmlogo_128x128.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href := Page.Urls.HOME,
      onEvent(
        "click",
        e =>
          e.preventDefault()
          Router.ChangeLocation(Page.Urls.HOME)
      )
    )(
      img(
        `class` := "home-logo",
        src     := logoImage,
        alt     := "Jobsboard logo"
      )
    )

  private def renderNavLinks(): List[Html[App.Msg]] =
    val constantLinks = List(renderSimpleNavLink("Jobs", Page.Urls.JOBS))

    val unauthedLinks = List(
      renderSimpleNavLink("Login", Page.Urls.LOGIN),
      renderSimpleNavLink("Sign up", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      renderNavLink("Log out", Page.Urls.HASH)(_ => Session.LogOut)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )

  private def renderSimpleNavLink(text: String, location: String) =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  private def renderNavLink(text: String, location: String)(location2msg: String => App.Msg) =
    li(`class` := "nav-item")(
      a(
        href    := location,
        `class` := "nav-link",
        onEvent(
          "click",
          e =>
            e.preventDefault()
            location2msg(location)
        )
      )(text)
    )
