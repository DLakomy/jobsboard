package dlakomy.jobsboard.core

import cats.effect.IO
import fs2.dom.History
import tyrian.*


final case class Router(location: String, history: History[IO, String]):
  import Router.*

  def update(msg: Msg): (Router, Cmd[IO, Msg]) = msg match
    case ChangeLocation(newLocation, browserTriggered) =>
      if (location == newLocation) (this, Cmd.None)
      else
        val historyCmd =
          // to avoid double history entry
          if (browserTriggered) Cmd.None
          else goto(newLocation)
        (this.copy(location = newLocation), historyCmd)
    case _ => (this, Cmd.None) // TODO external redirects

  def goto[M](location: String): Cmd[IO, M] =
    Cmd.SideEffect[IO]:
      history.pushState(location, location)


object Router:
  trait Msg
  case class ChangeLocation(location: String, browserTriggered: Boolean = false) extends Msg
  case class ExternalRedirect(location: String)                                  extends Msg

  def startAt[M](initialLocation: String): (Router, Cmd[IO, M]) =
    val router = Router(initialLocation, History[IO, String])
    (router, router.goto(initialLocation))
