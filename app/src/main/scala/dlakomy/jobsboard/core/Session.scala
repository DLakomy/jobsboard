package dlakomy.jobsboard.core

import cats.effect.IO
import tyrian.*
import tyrian.cmds.Logger


final case class Session(email: Option[String] = None, token: Option[String] = None):
  import Session.*

  def update(msg: Msg): (Session, Cmd[IO, Msg]) = msg match
    case SetToken(e, t) =>
      (this.copy(email = Some(e), token = Some(t)), Logger.consoleLog(s"Setting user session: $e - $t"))

  def initCmd: Cmd[IO, Msg] = Logger.consoleLog("Starting session monitor")


object Session:
  trait Msg
  case class SetToken(email: String, token: String) extends Msg
