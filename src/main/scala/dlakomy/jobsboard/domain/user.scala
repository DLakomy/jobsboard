package dlakomy.jobsboard.domain

import doobie.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.*


object user:
  final case class User(
      email: String,
      hashedPassword: String,
      firstName: Option[String],
      lastName: Option[String],
      company: Option[String],
      role: Role
  ) derives Read

  enum Role:
    case ADMIN, RECRUITER

  object Role:
    given Meta[Role] = Meta[String].imap(Role.valueOf)(_.toString)
