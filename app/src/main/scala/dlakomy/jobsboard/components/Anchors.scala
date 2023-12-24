package dlakomy.jobsboard.components

import dlakomy.jobsboard
import dlakomy.jobsboard.App
import dlakomy.jobsboard.core.Router
import tyrian.Html.*
import tyrian.*


object Anchors:
  def renderSimpleNavLink(text: String, location: String, cssClass: String = ""): Html[Router.Msg] =
    renderNavLink(text, location, cssClass)(Router.ChangeLocation(_))

  def renderNavLink[M <: App.Msg](text: String, location: String, cssClass: String = "")(
      location2msg: String => M
  ): Html[M] =
    a(
      href    := location,
      `class` := cssClass,
      onEvent(
        "click",
        e =>
          e.preventDefault()
          location2msg(location)
      )
    )(text)
