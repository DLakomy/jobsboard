package dlakomy.jobsboard.components

import dlakomy.jobsboard.App
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.pages.*
import tyrian.Html.*
import tyrian.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*


object Header:

  def view() =
    div(`class` := "container-fluid p-0")(
      div(`class` := "jvm-nav")(
        div(`class` := "container")(
          nav(`class` := "navbar navbar-expand-lg navbar-light JVM-nav")(
            div(`class` := "container")(
              renderLogo(),
              button(
                `class` := "navbar-toggler",
                `type`  := "button",
                attribute("data-bs-toggle", "collapse"),
                attribute("data-bs-target", "#navbarNav"),
                attribute("aria-controls", "navbarNav"),
                attribute("aria-expanded", "false"),
                attribute("aria-label", "Toggle navigation")
              )(
                span(`class` := "navbar-toggler-icon")()
              ),
              div(`class` := "collapse navbar-collapse", id := "navbarNav")(
                ul(
                  `class` := "navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3"
                )(
                  renderNavLinks()
                )
              )
            )
          )
        )
      )
    )

  /////////////////////////// private
  @js.native
  @JSImport("/static/img/rtjvmlogo_128x128.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href    := Page.urls.HOME,
      `class` := "navbar-brand",
      onEvent(
        "click",
        e =>
          e.preventDefault()
          Router.ChangeLocation(Page.urls.HOME)
      )
    )(
      img(
        `class` := "home-logo",
        src     := logoImage,
        alt     := "Jobsboard logo"
      )
    )

  private def renderNavLinks(): List[Html[App.Msg]] =
    val constantLinks = List(
      renderSimpleNavLink("Jobs", Page.urls.JOBS),
      renderSimpleNavLink("Post job", Page.urls.POST_JOB)
    )

    val unauthedLinks = List(
      renderSimpleNavLink("Login", Page.urls.LOGIN),
      renderSimpleNavLink("Sign up", Page.urls.SIGNUP)
    )

    val authedLinks = List(
      renderSimpleNavLink("Profile", Page.urls.PROFILE),
      renderNavLink("Log out", Page.urls.HASH)(_ => Session.LogOut)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )

  private def renderSimpleNavLink(text: String, location: String): Html[Router.Msg] =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  private def renderNavLink[M <: App.Msg](text: String, location: String)(location2msg: String => M): Html[M] =
    li(`class` := "nav-item")(
      Anchors.renderNavLink(
        text,
        location,
        "nav-link jvm-item"
      )(location2msg)
    )
