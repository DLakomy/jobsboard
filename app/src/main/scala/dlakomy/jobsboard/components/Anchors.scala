package dlakomy.jobsboard.components

import dlakomy.jobsboard
import dlakomy.jobsboard.App
import dlakomy.jobsboard.core.Router
import tyrian.Html.*
import tyrian.*


object Anchors:
  def renderSimpleNavLink(text: String, location: String): Html[Router.Msg] =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  def renderNavLink[M <: App.Msg](text: String, location: String)(location2msg: String => M): Html[M] =
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
