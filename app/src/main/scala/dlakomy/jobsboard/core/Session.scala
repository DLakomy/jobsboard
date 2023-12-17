package dlakomy.jobsboard.core

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.pages.Page
import org.scalajs.dom.document
import tyrian.*
import tyrian.http.*

import scala.scalajs.js.Date


final case class Session(email: Option[String] = None, token: Option[String] = None):
  import Session.*

  def update(msg: Msg): (Session, Cmd[IO, Msg | Router.Msg]) = msg match
    case SetToken(e, t, isNewLogin) =>
      val cookieCmd = Commands.setAllSessionCookies(e, t, isNewLogin)
      val routingCmd =
        if (isNewLogin) Cmd.Emit(Router.ChangeLocation(Page.urls.HOME))
        else Commands.checkToken()

      (this.copy(email = Some(e), token = Some(t)), cookieCmd |+| routingCmd)
    case LogOut =>
      val cmd = token.map(_ => Commands.logout()).getOrElse(Cmd.None)
      (this, cmd)
    case LogoutSuccess | InvalidateToken =>
      (
        this.copy(email = None, token = None),
        Commands.clearAllSessionCookies() |+|
          Cmd.Emit(Router.ChangeLocation(Page.urls.HOME))
      )
    case NoOp =>
      (this, Cmd.None)

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
  case object NoOp extends Msg
  // token actions
  case class SetToken(email: String, token: String, isNewLogin: Boolean) extends Msg
  case object InvalidateToken                                            extends Msg
  // loguout actions
  case object LogOut        extends Msg
  case object LogoutSuccess extends Msg
  case object LogoutError   extends Msg

  def isActive = getUserToken().nonEmpty

  def getUserToken() = getCookie(Constants.cookies.token)

  object Endpoints:
    val logout = new Endpoint[Msg]:
      val location                    = Constants.endpoints.logout
      val method                      = Method.Post
      val onResponse: Response => Msg = _ => LogoutSuccess
      val onError: HttpError => Msg   = _ => LogoutError

    val checkToken = new Endpoint[Msg]:
      val location = Constants.endpoints.checkToken
      val method   = Method.Get
      val onResponse: Response => Msg = response =>
        response.status match
          case Status(200, _) => NoOp
          case _              => InvalidateToken
      val onError: HttpError => Msg =
        _ => InvalidateToken

  object Commands:
    def logout(): Cmd[IO, Msg] =
      Endpoints.logout.callAuthorized()

    def checkToken(): Cmd[IO, Msg] =
      Endpoints.checkToken.callAuthorized()

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
