package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.domain.job.*
import dlakomy.jobsboard.pages.Page.StatusKind
import io.circe.generic.auto.*
import laika.api.*
import laika.format.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*


final case class JobPage(
    id: String,
    maybeJob: Option[Job] = None,
    status: Page.Status = Page.Status.LOADING
) extends Page:
  import JobPage.*

  def initCmd: Cmd[IO, Page.Msg] =
    Commands.getJob(id)

  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case SetErrorStatus(e) =>
      (setErrorStatus(e), Cmd.None)
    case SetJob(job) =>
      (setSuccessStatus("Success").copy(maybeJob = Some(job)), Cmd.None)
    case _ =>
      (this, Cmd.None)

  def view(): Html[Page.Msg] = maybeJob match
    case Some(job) => renderJobPage(job)
    case None      => renderNoJobPage()

  //////////////////////////////////////////
  // private
  //////////////////////////////////////////
  // ui
  private def renderJobPage(job: Job) =
    div(`class` := "job-page")(
      div(`class` := "job-hero")(
        img(
          `class` := "job-logo",
          src     := job.jobInfo.image.getOrElse(""),
          alt     := job.jobInfo.title
        ),
        h1(s"${job.jobInfo.company} - ${job.jobInfo.title}")
      ),
      div(`class` := "job-overview")(
        renderJobDetails(job)
      ),
      renderJobDescription(job),
      a(href := job.jobInfo.externalUrl, `class` := "job-apply-action", target := "blank")("Apply")
    )

  private def renderJobDetails(job: Job) =
    def renderDetail(value: String) =
      if (value.isEmpty) div("")
      else li(`class` := "job-detail-value")(value)

    val fullLocationString = job.jobInfo.country match
      case Some(country) => s"${job.jobInfo.location}, $country"
      case None          => job.jobInfo.location

    val currency = job.jobInfo.currency.getOrElse("")
    val fullSalaryString = (job.jobInfo.salaryLo, job.jobInfo.salaryHi) match
      case (Some(lo), Some(hi)) =>
        s"$currency $lo-$hi"
      case (Some(lo), None) =>
        s"> $currency $lo"
      case (None, Some(hi)) =>
        s"up to $currency $hi"
      case _ => "unspecified salary"

    div(`class` := "job-details")(
      ul(`class` := "job-detail")(
        renderDetail(fullLocationString),
        renderDetail(fullSalaryString),
        renderDetail(job.jobInfo.seniority.getOrElse("all levels")),
        renderDetail(job.jobInfo.tags.getOrElse(List()).mkString(","))
      )
    )

  private def renderJobDescription(job: Job) =
    val descriptionHtml = markdownTransformer.transform(job.jobInfo.description) match
      case Left(e) =>
        """Error when rendering markdown.
          |You can apply anyway and indicate them the problem""".stripMargin
      case Right(html) => html

    div(`class` := "job-description")().innerHtml(descriptionHtml)

  private def renderNoJobPage() = status.kind.match
    case StatusKind.LOADING => div("Loading.")
    case StatusKind.ERROR   => div("The job doesn't exist.")
    case StatusKind.SUCCESS => div("Huh. This error is unexpected.")

  // util
  val markdownTransformer = Transformer.from(Markdown).to(HTML).build

  private def setErrorStatus(message: String) =
    this.copy(status = Page.Status(message, Page.StatusKind.ERROR))

  private def setSuccessStatus(message: String) =
    this.copy(status = Page.Status(message, Page.StatusKind.SUCCESS))


object JobPage:
  trait Msg                                extends Page.Msg
  case class SetErrorStatus(error: String) extends Msg
  case class SetJob(job: Job)              extends Msg

  object Endpoints:
    def getJob(id: String) = new Endpoint[Msg]:
      override val location: String          = Constants.endpoints.jobs + s"/$id"
      override val method: Method            = Method.Get
      override val onError: HttpError => Msg = e => SetErrorStatus(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[Job, Msg](
          SetJob(_),
          SetErrorStatus(_)
        )

  object Commands:
    def getJob(id: String): Cmd[IO, Msg] =
      Endpoints.getJob(id).call()
