package dlakomy.jobsboard.http.validation

import cats.*
import cats.data.Validated.*
import cats.data.*
import cats.implicits.*
import dlakomy.jobsboard.domain.auth.*
import dlakomy.jobsboard.domain.job.*

import java.net.URL
import scala.util.Failure
import scala.util.Success
import scala.util.Try


object validators:

  sealed trait ValidationFailure(val errorMessage: String)
  case class EmptyField(fieldName: String)   extends ValidationFailure(s"'$fieldName' is empty")
  case class InvalidUrl(fieldName: String)   extends ValidationFailure(s"'$fieldName' is not a valid URL")
  case class InvalidEmail(fieldName: String) extends ValidationFailure(s"'$fieldName' is not a valid email")

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  trait Validator[A]:
    def validate(value: A): ValidationResult[A]

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  def validateRequired[A](field: A, fieldName: String)(required: A => Boolean): ValidationResult[A] =
    if (required(field)) field.validNel
    else EmptyField(fieldName).invalidNel

  def validateUrl(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI()) match
      case Success(_) => field.validNel
      case Failure(e) => InvalidUrl(fieldName).invalidNel

  def validateEmail(field: String, fieldName: String): ValidationResult[String] =
    if (emailRegex.findFirstMatchIn(field).isDefined) field.validNel
    else InvalidEmail(fieldName).invalidNel

  given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) =>
    val JobInfo(
      company,
      title,
      description,
      externalUrl,
      remote,
      location,
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other
    ) = jobInfo

    val validCompany     = validateRequired(company, "company")(_.nonEmpty)
    val validTitle       = validateRequired(title, "title")(_.nonEmpty)
    val validDescrption  = validateRequired(description, "description")(_.nonEmpty)
    val validExternalUrl = validateUrl(externalUrl, "externalUrl")
    val validLocation    = validateRequired(location, "location")(_.nonEmpty)

    (
      validCompany,       // company
      validTitle,         // title
      validDescrption,    // description
      validExternalUrl,   // externalUrl
      remote.validNel,    // remote
      validLocation,      // location
      salaryLo.validNel,  // salaryLo
      salaryHi.validNel,  // salaryHi
      currency.validNel,  // currency
      country.validNel,   // country
      tags.validNel,      // tags
      image.validNel,     // image
      seniority.validNel, // seniority
      other.validNel      // other
    ).mapN(JobInfo.apply)

  given loginInfoValidator: Validator[LoginInfo] = (loginInfo: LoginInfo) =>
    val validUserEmail = validateRequired(loginInfo.email, "email")(_.nonEmpty)
      .andThen(e => validateEmail(e, "email"))
    val validUserPassword = validateRequired(loginInfo.password, "password")(_.nonEmpty)
    (validUserEmail, validUserPassword).mapN(LoginInfo.apply)

  given newUserInfoValidator: Validator[NewUserInfo] = (newUserInfo: NewUserInfo) =>
    val validUserEmail = validateRequired(newUserInfo.email, "email")(_.nonEmpty)
      .andThen(e => validateEmail(e, "email"))

    // seems to be safe enough :D
    val validUserPassword = validateRequired(newUserInfo.password, "password")(_.nonEmpty)

    (
      validUserEmail,
      validUserPassword,
      newUserInfo.firstName.validNel,
      newUserInfo.lastName.validNel,
      newUserInfo.company.validNel
    ).mapN(NewUserInfo.apply)

  given newPasswordInfoValidator: Validator[NewPasswordInfo] = (newPasswordInfo: NewPasswordInfo) =>

    val validOldPassword = validateRequired(newPasswordInfo.oldPassword, "old password")(_.nonEmpty)
    val validNewPassword = validateRequired(newPasswordInfo.newPassword, "new password")(_.nonEmpty)

    (
      validOldPassword,
      validNewPassword
    ).mapN(NewPasswordInfo.apply)
