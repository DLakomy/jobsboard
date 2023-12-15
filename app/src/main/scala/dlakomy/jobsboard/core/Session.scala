package dlakomy.jobsboard.core

import cats.effect.IO
import dlakomy.jobsboard.common.Constants
import org.scalajs.dom.document
import tyrian.*

import scala.scalajs.js.Date


final case class Session(email: Option[String] = None, token: Option[String] = None):
  import Session.*

  def update(msg: Msg): (Session, Cmd[IO, Msg]) = msg match
    case SetToken(e, t, isNewLogin) =>
      (this.copy(email = Some(e), token = Some(t)), Commands.setAllSessionCookies(e, t, isNewLogin))

  def initCmd: Cmd[IO, Msg] =
    val maybeCommand = for
      email <- getCookie(Constants.cookies.email)
      token <- getCookie(Constants.cookies.token)
    yield Cmd.Emit(SetToken(email, token, isNewLogin = false))
    // isNewLogin = false, cuz if got cookie it's not a new session
    // I'd eat a cookie btw., need to buy some later

    maybeCommand.getOrElse(Cmd.None)


object Session:
  trait Msg
  case class SetToken(email: String, token: String, isNewLogin: Boolean) extends Msg

  object Commands:
    def setSessionCookie(name: String, value: String, isFresh: Boolean): Cmd[IO, Msg] =
      Cmd.SideEffect:
        if (getCookie(name).isEmpty || isFresh)
          document.cookie = s"$name=$value;expires=${new Date(Date.now() + Constants.cookies.duration)};path=/"

    def setAllSessionCookies(email: String, token: String, isFresh: Boolean = false): Cmd[IO, Msg] =
      setSessionCookie(Constants.cookies.email, email, isFresh) |+|
        setSessionCookie(Constants.cookies.token, token, isFresh)

    def clearSessionCookie(name: String): Cmd[IO, Msg] =
      Cmd.SideEffect:
        document.cookie = s"$name=;expires=${new Date(0)};path=/"

    def clearAllSessionCookies(): Cmd[IO, Msg] =
      clearSessionCookie(Constants.cookies.email) |+|
        clearSessionCookie(Constants.cookies.token)

  private def getCookie(name: String): Option[String] =
    document.cookie
      .split(";")
      .map(_.trim)
      .find(_.startsWith(s"$name="))
      .map(_.split("="))
      .map(_(1))
