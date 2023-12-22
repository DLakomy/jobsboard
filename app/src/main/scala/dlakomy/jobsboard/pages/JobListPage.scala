package dlakomy.jobsboard.pages

import cats.effect.IO
import dlakomy.jobsboard
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.components.FilterPanel
import dlakomy.jobsboard.domain.job.*
import dlakomy.jobsboard.pages.Page.Msg
import io.circe.generic.auto.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*


final case class JobListPage(
    filterPanel: FilterPanel = FilterPanel(filterAction = FilterJobs(_)),
    jobs: List[Job] = List.empty,
    jobFilter: JobFilter = JobFilter(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status("Loading", Page.StatusKind.LOADING))
) extends Page:
  import JobListPage.*

  def initCmd: Cmd[IO, Page.Msg] =
    filterPanel.initCmd |+| Commands.getJobs()

  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = msg match
    case AddJobs(lst, clm) =>
      (setSuccessStatus("Loaded").copy(jobs = this.jobs ++ lst, canLoadMore = clm), Cmd.None)
    case SetErrorStatus(e) =>
      (setErrorStatus(e), Cmd.None)
    case LoadMoreJobs =>
      (this, Commands.getJobs(filter = jobFilter, offset = jobs.length))
    case FilterJobs(selectedFilters) =>
      val newJobFilter = createJobFilter(selectedFilters)
      (this.copy(jobs = List(), jobFilter = newJobFilter), Commands.getJobs(filter = newJobFilter))
    case msg: FilterPanel.Msg =>
      val (newFilterPanel, cmd) = filterPanel.update(msg)
      (this.copy(filterPanel = newFilterPanel), cmd)
    case _ => (this, Cmd.None)

  def view(): Html[Page.Msg] =
    div(`class` := "job-list-page")(
      div(`class` := "filter-panel-container")(
        filterPanel.view()
      ),
      div(`class` := "jobs-container")(
        jobs.map(renderJob) ++ maybeRenderLoadMore
      )
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

  private def createJobFilter(selectedFilters: Map[String, Set[String]]) =
    JobFilter(
      companies = selectedFilters.get("Companies").getOrElse(Set()).toList,
      locations = selectedFilters.get("Locations").getOrElse(Set()).toList,
      countries = selectedFilters.get("Countries").getOrElse(Set()).toList,
      seniorities = selectedFilters.get("Seniorities").getOrElse(Set()).toList,
      tags = selectedFilters.get("Tags").getOrElse(Set()).toList,
      maxSalary = Some(filterPanel.maxSalary),
      remoteOnly = filterPanel.remoteOnly
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
  // action
  case object LoadMoreJobs                                         extends Msg
  case class FilterJobs(selectedFilters: Map[String, Set[String]]) extends Msg

  object Endpoints:
    def getJobs(limit: Int = Constants.defaultPageSize, offset: Int = 0) = new Endpoint[Msg]:
      override val location: String          = Constants.endpoints.jobs + s"?limit=$limit&offset=$offset"
      override val method: Method            = Method.Post
      override val onError: HttpError => Msg = e => SetErrorStatus(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[List[Job], Msg](
          list =>
            AddJobs(
              list,
              canLoadMore = !list.isEmpty
            ),
          SetErrorStatus(_)
        )

  object Commands:
    def getJobs(
        filter: JobFilter = JobFilter(),
        limit: Int = Constants.defaultPageSize,
        offset: Int = 0
    ): Cmd[IO, Msg] =
      Endpoints.getJobs(limit, offset).call(filter)
