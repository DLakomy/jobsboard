package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.domain.job.*
import dlakomy.jobsboard.pages.Page.Msg
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*


final case class JobListPage(
    jobs: List[Job] = List.empty,
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status("Loading", Page.StatusKind.LOADING))
) extends Page:
  import JobListPage.*

  def initCmd: Cmd[IO, Page.Msg] =
    Commands.getJobs()

  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case AddJobs(lst, clm) =>
      (setSuccessStatus("Loaded").copy(jobs = this.jobs ++ lst, canLoadMore = clm), Cmd.None)
    case SetErrorStatus(e) =>
      (setErrorStatus(e), Cmd.None)
    case LoadMoreJobs =>
      (this, Commands.getJobs(offset = jobs.length))
    case _ => (this, Cmd.None)

  def view(): Html[Page.Msg] =
    div(`class` := "jobs-container")(
      jobs.map(renderJob) ++ maybeRenderLoadMore
    )

  //////////////////////////////////////////
  // private
  //////////////////////////////////////////
  // ui
  private def renderJob(job: Job) =
    div(`class` := "job-card")(
      div(`class` := "job-card-img")(
        img(
          `class` := "job-logo",
          src     := job.jobInfo.image.getOrElse(""),
          alt     := job.jobInfo.title
        )
      ),
      div(`class` := "job-card-content")(
        h4(s"${job.jobInfo.company} - ${job.jobInfo.title}")
      ),
      div(`class` := "job-card-apply")(
        a(href := job.jobInfo.externalUrl, target := "blank")("Apply")
      )
    )

  private def maybeRenderLoadMore = status.map: s =>
    div(`class` := "load-more-action")(
      s match
        case Page.Status(_, Page.StatusKind.LOADING) => div("Loading...")
        case Page.Status(e, Page.StatusKind.ERROR)   => div(e)
        case Page.Status(_, Page.StatusKind.SUCCESS) =>
          if (canLoadMore)
            button(`type` := "button", onClick(LoadMoreJobs))("Load more")
          else
            div("All jobs loaded")
    )

  // util
  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))


object JobListPage:
  trait Msg                                                 extends Page.Msg
  case class SetErrorStatus(error: String)                  extends Msg
  case class AddJobs(list: List[Job], canLoadMore: Boolean) extends Msg
  case object LoadMoreJobs                                  extends Msg
  // action

  object Endpoint:
    def getJobs(limit: Int = Constants.defaultPageSize, offset: Int = 0) = new Endpoint[Msg]:
      override val location: String          = Constants.endpoints.jobs + s"?limit=$limit&offset=$offset"
      override val method: Method            = Method.Post
      override val onError: HttpError => Msg = e => SetErrorStatus(e.toString)
      override val onResponse: Response => Msg = response =>
        response.status match
          case Status(s, _) if s >= 200 && s < 300 =>
            val json   = response.body
            val parsed = parse(json).flatMap(_.as[List[Job]])
            parsed match
              case Left(parsingError) => SetErrorStatus(s"Parsing error: $parsingError")
              case Right(list)        => AddJobs(list, canLoadMore = offset == 0 || !list.isEmpty)
          case Status(code, message) if code >= 400 && code < 600 =>
            SetErrorStatus(s"Error: $message")

  object Commands:
    def getJobs(
        filter: JobFilter = JobFilter(),
        limit: Int = Constants.defaultPageSize,
        offset: Int = 0
    ): Cmd[IO, Msg] =
      Endpoint.getJobs(limit, offset).call(filter)
