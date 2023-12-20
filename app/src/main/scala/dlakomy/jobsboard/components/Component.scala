package dlakomy.jobsboard.components

import cats.effect.IO
import tyrian.*


trait Component[-MsgIn, +MsgOut, +Model]:
  def initCmd: Cmd[IO, MsgOut]
  def update(msg: MsgIn): (Model, Cmd[IO, MsgOut])
  def view(): Html[MsgOut]
