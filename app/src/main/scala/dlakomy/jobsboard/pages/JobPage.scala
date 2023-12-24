package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.components.JobComponents
import dlakomy.jobsboard.domain.job.*
import dlakomy.jobsboard.pages.Page.StatusKind
import io.circe.generic.auto.*
import laika.api.*
import laika.format.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*

import scala.scalajs.*
import scala.scalajs.js.*
import scala.scalajs.js.annotation.*


@js.native
@JSGlobal()
class Moment extends js.Object:
  def fromNow(): String = js.native


@js.native
@JSImport("moment", JSImport.Default)
object MomentLib extends js.Object:
  def unix(date: Long): Moment = js.native


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
    div(`class` := "container-fluid the-rock")(
      div(`class` := "row jvm-jobs-details-top-card")(
        div(`class` := "col-md-12 p-0")(
          div(`class` := "jvm-jobs-details-card-profile-img")(
            img(
              `class` := "img-fluid",
              src     := job.jobInfo.image.getOrElse(""),
              alt     := job.jobInfo.title
            )
          ),
          div(`class` := "jvm-jobs-details-card-profile-title")(
            h1(s"${job.jobInfo.company} - ${job.jobInfo.title}"),
            div(`class` := "jvm-jobs-details-card-profile-job-details-company-and-location")(
              JobComponents.renderJobSummary(job)
            )
          ),
          div(`class` := "jvm-jobs-details-card-apply-now-btn")(
            a(href := job.jobInfo.externalUrl, target := "blank")(
              button(`type` := "button", `class` := "btn btn-warning")("Apply now")
            ),
            p(
              MomentLib.unix(job.date / 1000).fromNow()
            )
          )
        )
      ),
      div(`class` := "container-fluid")(
        div(`class` := "container")(
          div(`class` := "markdown-body overview-section")(
            renderJobDescription(job)
          )
        ),
        div(`class` := "container")(
          div(`class` := "rok-last")(
            div(`class` := "row")(
              div(`class` := "col-md-6 col-sm-6 col-6")(
                span(`class` := "rock-apply")("Apply for this job.")
              ),
              div(`class` := "col-md-6 col-sm-6 col-6")(
                a(href := job.jobInfo.externalUrl, target := "blank")(
                  button(`type` := "button", `class` := "rock-apply-btn")("Apply now")
                )
              )
            )
          )
        )
      )
    )

  private def renderJobDescription(job: Job) =
    val descriptionHtml = markdownTransformer.transform(job.jobInfo.description) match
      case Left(e) =>
        """Error when rendering markdown.
          |You can apply anyway and indicate them the problem""".stripMargin
      case Right(html) => html

    div(`class` := "job-description")().innerHtml(descriptionHtml)

  private def renderNoJobPage() =
    div(`class` := "container-fluid the-rock")(
      div(`class` := "row jvm-jobs-details-top-card")(
        status.kind.match
          case StatusKind.LOADING => h1("Loading.")
          case StatusKind.ERROR   => h1("The job doesn't exist.")
          case StatusKind.SUCCESS => h1("Huh. This error is unexpected.")
      )
    )
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
