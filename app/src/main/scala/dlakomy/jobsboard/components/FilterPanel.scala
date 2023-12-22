package dlakomy.jobsboard.components

import cats.effect.IO
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.domain.job.JobFilter
import dlakomy.jobsboard.pages.Page
import io.circe.generic.auto.*
import org.scalajs.dom.HTMLInputElement
import tyrian.Html.*
import tyrian.*
import tyrian.cmds.Logger
import tyrian.http.*


final case class FilterPanel(
    possibleFilters: JobFilter = JobFilter(),
    selectedFilters: Map[String, Set[String]] = Map(),
    maybeError: Option[String] = None,
    maxSalary: Int = 0,
    remoteOnly: Boolean = false,
    dirty: Boolean = false,
    filterAction: Map[String, Set[String]] => Page.Msg = _ => Page.NoOp
) extends Component[FilterPanel.Msg, Page.Msg, FilterPanel]:
  import FilterPanel.*

  override def initCmd: Cmd[IO, Page.Msg] =
    Commands.getFilters

  override def update(msg: FilterPanel.Msg): (FilterPanel, Cmd[IO, Page.Msg]) = msg match
    case TriggerFilter =>
      (this.copy(dirty = false), Cmd.Emit(filterAction(selectedFilters)))
    case SetPossibleFilters(pF) =>
      (this.copy(possibleFilters = pF), Cmd.None)
    case FilterPanelError(e) =>
      (this.copy(maybeError = Some(e)), Cmd.None)
    case UpdateSalaryInput(salary) =>
      (this.copy(maxSalary = salary, dirty = true), Cmd.None)
    case UpdateValueChecked(groupName, value, checked) =>
      val oldGroup  = selectedFilters.get(groupName).getOrElse(Set())
      val newGroup  = if (checked) oldGroup + value else oldGroup - value
      val newGroups = selectedFilters + (groupName -> newGroup)
      (this.copy(selectedFilters = newGroups, dirty = true), Logger.consoleLog(s"Filters: $newGroups"))
    case UpdateRemote(checked) =>
      (this.copy(remoteOnly = checked, dirty = true), Cmd.None)

  override def view(): Html[Page.Msg] =
    div(`class` := "filter-panel-container")(
      maybeRenderError(),
      renderSalaryFilter(),
      renderRemoteCheckbox(),
      renderCheckboxGroup("Companies", possibleFilters.companies),
      renderCheckboxGroup("Locations", possibleFilters.locations),
      renderCheckboxGroup("Countries", possibleFilters.countries),
      renderCheckboxGroup("Tags", possibleFilters.tags),
      renderCheckboxGroup("Seniorities", possibleFilters.seniorities),
      renderApplyFiltersBtn()
    )

  ///////////////////// private
  /////////////////////////////
  private def maybeRenderError() =
    maybeError
      .map: e =>
        div(`class` := "filter-panel-error")(e)
      .getOrElse(div())

  private def renderSalaryFilter() =
    div(`class` := "filter-group")(
      h6(`class` := "filter-group-header")("Salary"),
      div(`class` := "filter-group-content")(
        label(`for` := "filter-salary")("Min (in local currrency)"),
        input(`type` := "number", id := "salary", onInput(s => UpdateSalaryInput(if (s.isEmpty) 0 else (s.toInt))))
      )
    )

  private def renderRemoteCheckbox() =
    div(`class` := "filter-group-content")(
      label(`for` := s"filter-remote")("Remote only"),
      input(
        `type` := "checkbox",
        id     := s"filter-remote",
        checked(remoteOnly),
        onEvent(
          "change",
          e =>
            val checkbox = e.target.asInstanceOf[HTMLInputElement]
            UpdateRemote(checkbox.checked)
        )
      )
    )

  private def renderCheckboxGroup(groupName: String, values: List[String]) =
    val selectedValues = selectedFilters.get(groupName).getOrElse(Set())
    div(`class` := "filter-group")(
      h6(`class` := "filter-group-header")(groupName),
      div(`class` := "filter-group-content")(
        values.map(value => renderCheckbox(groupName, value, selectedValues))
      )
    )

  private def renderCheckbox(groupName: String, value: String, selectedValues: Set[String]) =
    div(`class` := "filter-group-content")(
      label(`for` := s"filter-$groupName-$value")(value),
      input(
        `type` := "checkbox",
        id     := s"filter-$groupName-$value",
        checked(selectedValues.contains(value)),
        onEvent(
          "change",
          e =>
            val checkbox = e.target.asInstanceOf[HTMLInputElement]
            UpdateValueChecked(groupName, value, checkbox.checked)
        )
      )
    )

  private def renderApplyFiltersBtn() =
    button(`type` := "button", disabled(!dirty), onClick(TriggerFilter))("Apply filters")


object FilterPanel:
  sealed trait Msg                                          extends Page.Msg
  case object TriggerFilter                                 extends Msg
  case class FilterPanelError(error: String)                extends Msg
  case class SetPossibleFilters(possibleFilters: JobFilter) extends Msg
  // content
  case class UpdateSalaryInput(salary: Int)                                         extends Msg
  case class UpdateValueChecked(groupName: String, value: String, checked: Boolean) extends Msg
  case class UpdateRemote(checked: Boolean)                                         extends Msg

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
