package dlakomy.jobsboard.fixtures

import dlakomy.jobsboard.domain.user.*

trait UsersFixture:
  val dawid =
    User(
      "dawid@dlakomy.github.io",
      "$2a$10$k7SC5Wz54II9QMrB7.FhEeYWApxNQH28tWGKcbtbkXTDE02yYq2Ba", // something
      Some("Dawid"),
      Some("Hungry"),
      Some("DL corp."),
      Role.ADMIN
    )
  val john =
    User(
      "john@lakomy.github.io",
      "$2a$10$yuC4.08NGHHkgAfuSE0ORee1uBQMqn5W5F5srhvWZMy9TnQH39kZS", // somethingelse
      Some("John"),
      Some("Hungrytoo"),
      Some("DL corp."),
      Role.RECRUITER
    )
  val newUser =
    User(
      "john@newman.github.io",
      "$2a$10$SbtdZxUhv9yqBXzJiI5Ud.tkesGl9BZ019RH6WmPAgnS6KI9WlyEi", // somethingelsish
      Some("John"),
      Some("Newman"),
      Some("Newman corp."),
      Role.RECRUITER
    )
  val updatedJohn =
    User(
      "john@lakomy.github.io",
      "$2a$10$ImCg./u9zleJcoWPhW4du.isvK283VMD7HLB0BdHA0qUo5mdrGmwO", // somethingelsebetter
      Some("John"),
      Some("Hungryn't"),
      Some("Adobe"),
      Role.RECRUITER
    )

  val dawidEmail        = dawid.email
  val dawidPassword     = "something"
  val dawidPasswordHash = dawid.hashedPassword
  val johnEmail         = john.email
  val johnPassword      = "somethingelse"

  val newUserDawid = NewUserInfo(dawidEmail, dawidPassword, dawid.firstName, dawid.lastName, dawid.company)
  val newUserJohn  = NewUserInfo(johnEmail, johnPassword, john.firstName, john.lastName, john.company)
