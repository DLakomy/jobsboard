package dlakomy.jobsboard.domain

import doobie.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.*
import tsec.authorization.AuthGroup
import tsec.authorization.SimpleAuthEnum


object user:
  final case class User(
      email: String,
      hashedPassword: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String],
      role: Role
  ) derives Read

  final case class NewUserInfo(
      email: String,
      password: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String]
  )

  enum Role:
    case ADMIN, RECRUITER

  object Role:
    given Meta[Role] = Meta[String].imap(Role.valueOf)(_.toString)

    given SimpleAuthEnum[Role, String] with
      override val values: AuthGroup[Role]  = AuthGroup(Role.ADMIN, Role.RECRUITER)
      override def getRepr(t: Role): String = t.toString
