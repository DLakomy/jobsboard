package dlakomy.jobsboard.components

import dlakomy.jobsboard.App
import dlakomy.jobsboard.core.Router
import dlakomy.jobsboard.domain.job.Job
import dlakomy.jobsboard.pages.Page
import tyrian.Html
import tyrian.Html.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*


object JobComponents:

  @js.native
  @JSImport("/static/img/nologo.png", JSImport.Default)
  private val noLogoImg: String = js.native

  def card(job: Job): Html[Router.Msg] =
    div(`class` := "jvm-recent-jobs-cards")(
      div(`class` := "jvm-recent-jobs-card-img")(
        img(
          `class` := "img-fluid",
          src     := job.jobInfo.image.getOrElse(""),
          alt     := job.jobInfo.title,
          attribute("onerror", s"this.src='$noLogoImg'")
        )
      ),
      div(`class` := "jvm-recent-jobs-card-contents")(
        h5(
          Anchors.renderSimpleNavLink(
            s"${job.jobInfo.company} - ${job.jobInfo.title}",
            Page.urls.JOB(job.id.toString),
            "job-title-link"
          )
        ),
        renderJobSummary(job)
      ),
      div(`class` := "jvm-recent-jobs-card-btn-apply")(
        a(href := job.jobInfo.externalUrl, target := "blank")(
          button(`type` := "button", `class` := "btn btn-danger")("Apply")
        )
      )
    )

  def renderJobSummary[M <: App.Msg](job: Job): Html[M] =
    import job.jobInfo.*
    div(
      renderDetail("dollar", fullSalaryString(job)),
      renderDetail("location-dot", fullLocationString(job)),
      maybeRenderDetail("ranking-star", seniority),
      maybeRenderDetail("tags", tags.map(_.mkString(", ")))
    )

  def renderDetail[M <: App.Msg](icon: String, value: String): Html[M] =
    div(
      div(`class` := "job-detail")(
        i(`class` := s"fa fa-$icon job-detail-icon")(),
        p(`class` := "job-detail-value")(value)
      )
    )

  def maybeRenderDetail[M <: App.Msg](icon: String, maybeValue: Option[String]): Html[M] =
    maybeValue.map(value => renderDetail(icon, value)).getOrElse(div("")) // I think newer Tyrian has Empty

  /////////////// private
  private def fullSalaryString(job: Job) =
    val currency = job.jobInfo.currency.getOrElse("")
    (job.jobInfo.salaryLo, job.jobInfo.salaryHi) match
      case (Some(lo), Some(hi)) =>
        s"$currency $lo-$hi"
      case (Some(lo), None) =>
        s"> $currency $lo"
      case (None, Some(hi)) =>
        s"up to $currency $hi"
      case _ => "unspecified = ∞"

  private def fullLocationString(job: Job) = job.jobInfo.country match
    case Some(country) => s"${job.jobInfo.location}, $country"
    case None          => job.jobInfo.location
