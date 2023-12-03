package dlakomy.jobsboard.fixtures

import dlakomy.jobsboard.domain.user.*

trait UsersFixture:
  val dawid =
    User("dawid@lakomy.github.io", "something", Some("Dawid"), Some("Hungry"), Some("DL corp."), Role.ADMIN)
  val john =
    User("john@lakomy.github.io", "somethingelse", Some("John"), Some("Hungrytoo"), Some("DL corp."), Role.RECRUITER)
  val newUser =
    User("john@newman.github.io", "somethingelsish", Some("John"), Some("Newman"), Some("Newman corp."), Role.RECRUITER)
  val updatedJohn =
    User("john@lakomy.github.io", "somethingelsebetter", Some("JOHN"), Some("HUNGRYN'T"), Some("Adobe"), Role.RECRUITER)

  val dawidEmail        = dawid.email
  val dawidPasswordHash = dawid.hashedPassword
  val johnEmail         = john.email
