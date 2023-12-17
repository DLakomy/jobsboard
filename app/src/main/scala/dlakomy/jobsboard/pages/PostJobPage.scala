package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.core.Session
import dlakomy.jobsboard.domain.job.JobInfo
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.cmds.Logger
import tyrian.http.*


final case class PostJobPage(
    company: String = "",
    title: String = "",
    description: String = "",
    externalUrl: String = "",
    remote: Boolean = false,
    location: String = "",
    salaryLo: Option[Int] = None,
    salaryHi: Option[Int] = None,
    currency: Option[String] = None,
    country: Option[String] = None,
    tags: Option[String] = None, // TODO parse before sending
    image: Option[String] = None,
    seniority: Option[String] = None,
    other: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Post job", status):

  import PostJobPage.*

  override def view() =
    // this logic could be present in the FormPage - we'll see if it's a reusable case
    if (Session.isActive) super.view()
    else renderInvalidPage()

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case UpdateCompany(v) =>
      (this.copy(company = v), Cmd.None)
    case UpdateTitle(v) =>
      (this.copy(title = v), Cmd.None)
    case UpdateDescription(v) =>
      (this.copy(description = v), Cmd.None)
    case UpdateExternalUrl(v) =>
      (this.copy(externalUrl = v), Cmd.None)
    case ToggleRemote =>
      (this.copy(remote = !this.remote), Cmd.None)
    case UpdateLocation(v) =>
      (this.copy(location = v), Cmd.None)
    case UpdateSalaryLo(v) =>
      (this.copy(salaryLo = Some(v)), Cmd.None)
    case UpdateSalaryHi(v) =>
      (this.copy(salaryHi = Some(v)), Cmd.None)
    case UpdateCurrency(v) =>
      (this.copy(currency = Some(v)), Cmd.None)
    case UpdateCountry(v) =>
      (this.copy(country = Some(v)), Cmd.None)
    // text input is comma separated
    case UpdateTags(v) =>
      (this.copy(tags = Some(v)), Cmd.None)
    case UpdateSeniority(v) =>
      (this.copy(seniority = Some(v)), Cmd.None)
    case UpdateOther(v) =>
      (this.copy(other = Some(v)), Cmd.None)
    case PostJobError(error) =>
      (setErrorStatus(error), Cmd.None)
    case PostJobSuccess(jobId) =>
      (setSuccessStatus("Success!"), Logger.consoleLog(s"Job id: $jobId"))
    case AttemptPostJob =>
      (
        this,
        Commands.postJob(
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
        )
      )

  override protected def renderFormContent(): List[Html[Page.Msg]] =
    List(
      renderInput("Company", "company", "text", true, UpdateCompany(_)),
      renderInput("Title", "title", "text", true, UpdateTitle(_)),
      renderTextArea("Description", "description", true, UpdateDescription(_)),
      renderInput("ExternalUrl", "externalUrl", "text", true, UpdateExternalUrl(_)),
      renderInput("Location", "location", "text", true, UpdateLocation(_)),
      renderInput("Remote", "remote", "checkbox", true, _ => ToggleRemote),
      renderInput("salaryLo", "salaryLo", "number", false, value => UpdateSalaryLo(value.toInt)),
      renderInput("salaryHi", "salaryHi", "number", false, value => UpdateSalaryHi(value.toInt)),
      renderInput("Currency", "currency", "text", false, UpdateCurrency(_)),
      renderInput("Country", "country", "text", false, UpdateCountry(_)),
      // TODO image upload
      renderInput("Tags", "tags", "text", false, UpdateTags(_)),
      renderInput("Seniority", "seniority", "text", false, UpdateSeniority(_)),
      renderInput("Other", "other", "text", false, UpdateOther(_)),
      button(`type` := "button", onClick(AttemptPostJob))("Post job")
    )

  //////////////////////////////////////////
  // private
  //////////////////////////////////////////
  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  private def renderInvalidPage() =
    div(h1("Profile"), div("You need to be logged in to view this page"))


object PostJobPage:
  trait Msg                                extends Page.Msg
  case class UpdateCompany(v: String)      extends Msg
  case class UpdateTitle(v: String)        extends Msg
  case class UpdateDescription(v: String)  extends Msg
  case class UpdateExternalUrl(v: String)  extends Msg
  case object ToggleRemote                 extends Msg
  case class UpdateLocation(v: String)     extends Msg
  case class UpdateSalaryLo(v: Int)        extends Msg
  case class UpdateSalaryHi(v: Int)        extends Msg
  case class UpdateCurrency(v: String)     extends Msg
  case class UpdateCountry(v: String)      extends Msg
  case class UpdateSeniority(v: String)    extends Msg
  case class UpdateOther(v: String)        extends Msg
  case class UpdateTags(v: String)         extends Msg
  case class PostJobSuccess(jobId: String) extends Msg
  case class PostJobError(error: String)   extends Msg
  // actions
  case object AttemptPostJob extends Msg

  object Endpoints:
    val postJob = new Endpoint[Msg]:
      override val location: String          = Constants.endpoints.postJob
      override val method: Method            = Method.Post
      override val onError: HttpError => Msg = e => PostJobError(e.toString)
      override val onResponse: Response => Msg = response =>
        response.status match
          case Status(s, _) if s >= 200 && s < 300 =>
            val jobId = response.body
            PostJobSuccess(jobId)
          case Status(s, _) if s >= 400 && s < 500 =>
            val json   = response.body
            val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
            parsed match
              case Left(e)                => PostJobError(s"Response error: ${e.getMessage}")
              case Right(errorFromServer) => PostJobError(errorFromServer)
          case _ =>
            PostJobError("Unknown reply from the server.")

  object Commands:
    def postJob(
        company: String,
        title: String,
        description: String,
        externalUrl: String,
        remote: Boolean,
        location: String,
        salaryLo: Option[Int],
        salaryHi: Option[Int],
        currency: Option[String],
        country: Option[String],
        tags: Option[String],
        image: Option[String],
        seniority: Option[String],
        other: Option[String]
    ): Cmd[IO, Msg] =
      Endpoints.postJob.callAuthorized(
        JobInfo(
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
          tags.map(_.split(',').map(_.trim).toList),
          image,
          seniority,
          other
        )
      )
