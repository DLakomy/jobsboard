package dlakomy.jobsboard.components

import cats.effect.IO
import dlakomy.jobsboard.core.*
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
          renderNavLink("Jobs", "/jobs"),
          renderNavLink("Login", "/login"),
          renderNavLink("Sign up", "/signup")
        )
      )
    )

  /////////////////////////// private
  @js.native
  @JSImport("/static/img/rtjvmlogo_128x128.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href := "/",
      onEvent(
        "click",
        e =>
          e.preventDefault()
          Router.ChangeLocation("/")
      )
    )(
      img(
        `class` := "home-logo",
        src     := logoImage,
        alt     := "Jobsboard logo"
      )
    )

  private def renderNavLink(text: String, location: String) =
    li(`class` := "nav-item")(
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
    )
