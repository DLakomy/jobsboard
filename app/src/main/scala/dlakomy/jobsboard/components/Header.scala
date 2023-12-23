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
      href := Page.urls.HOME,
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
      Anchors.renderSimpleNavLink("Jobs", Page.urls.JOBS),
      Anchors.renderSimpleNavLink("Post job", Page.urls.POST_JOB)
    )

    val unauthedLinks = List(
      Anchors.renderSimpleNavLink("Login", Page.urls.LOGIN),
      Anchors.renderSimpleNavLink("Sign up", Page.urls.SIGNUP)
    )

    val authedLinks = List(
      Anchors.renderSimpleNavLink("Profile", Page.urls.PROFILE),
      Anchors.renderNavLink("Log out", Page.urls.HASH)(_ => Session.LogOut)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )
