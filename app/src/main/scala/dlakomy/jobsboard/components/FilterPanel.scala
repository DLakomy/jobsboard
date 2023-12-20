package dlakomy.jobsboard.components

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.domain.job.JobFilter
import dlakomy.jobsboard.pages.Page
import io.circe.generic.auto.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*


final case class FilterPanel(possibleFilters: JobFilter = JobFilter(), maybeError: Option[String] = None)
    extends Component[Page.Msg, Page.Msg, FilterPanel]:
  import FilterPanel.*

  override def initCmd: Cmd[IO, Page.Msg] =
    Commands.getFilters

  override def update(msg: Page.Msg): (FilterPanel, Cmd[IO, Page.Msg]) = msg match
    case SetPossibleFilters(pF) =>
      (this.copy(possibleFilters = pF), Cmd.None)
    case FilterPanelError(e) =>
      (this.copy(maybeError = Some(e)), Cmd.None)
    case _ => (this, Cmd.None)

  override def view(): Html[Page.Msg] =
    div(`class` := "filter-panel-container")(
      maybeRenderError(),
      text(possibleFilters.toString)
    )

  ///////////////////// private
  /////////////////////////////
  private def maybeRenderError() =
    maybeError
      .map: e =>
        div(`class` := "filter-panel-error")(e)
      .getOrElse(div())


object FilterPanel:
  trait Msg                                                 extends Page.Msg
  case class FilterPanelError(error: String)                extends Msg
  case class SetPossibleFilters(possibleFilters: JobFilter) extends Msg

  object Endpoints:
    val getFilters = new Endpoint[Msg]:
      override val location: String          = Constants.endpoints.filters
      override val method: Method            = Method.Get
      override val onError: HttpError => Msg = e => FilterPanelError(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[JobFilter, Msg](
          SetPossibleFilters(_),
          FilterPanelError(_)
        )

  object Commands:
    def getFilters = Endpoints.getFilters.call()
