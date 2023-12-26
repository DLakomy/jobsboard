package dlakomy.jobsboard.pages

import cats.effect.IO
import cats.syntax.traverse.*
import dlakomy.jobsboard.common.*
import dlakomy.jobsboard.core.*
import dlakomy.jobsboard.domain.job.JobInfo
import io.circe.generic.auto.*
import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.File
import org.scalajs.dom.FileReader
import org.scalajs.dom.HTMLCanvasElement
import org.scalajs.dom.HTMLImageElement
import org.scalajs.dom.document
import tyrian.Html.*
import tyrian.*
import tyrian.http.*

import scala.util.Try


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
    tags: Option[String] = None,
    image: Option[String] = None,
    seniority: Option[String] = None,
    other: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Post job", status):

  import PostJobPage.*

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg | Router.Msg]) = msg match
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
      (this.copy(salaryLo = v), Cmd.None)
    case UpdateSalaryHi(v) =>
      (this.copy(salaryHi = v), Cmd.None)
    case UpdateCurrency(v) =>
      (this.copy(currency = Some(v)), Cmd.None)
    case UpdateCountry(v) =>
      (this.copy(country = Some(v)), Cmd.None)
    case UpdateImageFile(maybeFile) =>
      (this, Commands.loadFile(maybeFile))
    case UpdateImage(maybeImage) =>
      (this.copy(image = maybeImage), Cmd.None)
    case UpdateTags(v) =>
      (this.copy(tags = Some(v)), Cmd.None)
    case UpdateSeniority(v) =>
      (this.copy(seniority = Some(v)), Cmd.None)
    case UpdateOther(v) =>
      (this.copy(other = Some(v)), Cmd.None)
    case PostJobError(error) =>
      (setErrorStatus(error), Cmd.None)
    case PostJobSuccess(jobId) =>
      (setSuccessStatus("Success!"), Cmd.None)
    case AttemptPostJob =>
      (
        this,
        Commands.postJob(true)(
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
    if (!Session.isActive) renderInvalidContents()
    else
      List(
        renderInput("Company", "company", "text", true, UpdateCompany(_)),
        renderInput("Title", "title", "text", true, UpdateTitle(_)),
        renderTextArea("Description", "description", true, UpdateDescription(_)),
        renderInput("ExternalUrl", "externalUrl", "text", true, UpdateExternalUrl(_)),
        renderInput("Location", "location", "text", true, UpdateLocation(_)),
        renderToggle("Remote", "remote", true, _ => ToggleRemote),
        renderInput("salaryLo", "salaryLo", "number", false, s => UpdateSalaryLo(parseNumber(s))),
        renderInput("salaryHi", "salaryHi", "number", false, s => UpdateSalaryHi(parseNumber(s))),
        renderInput("Currency", "currency", "text", false, UpdateCurrency(_)),
        renderInput("Country", "country", "text", false, UpdateCountry(_)),
        renderImageUploadInput("Logo", "logo", image, UpdateImageFile(_)),
        renderInput("Tags", "tags", "text", false, UpdateTags(_)),
        renderInput("Seniority", "seniority", "text", false, UpdateSeniority(_)),
        renderInput("Other", "other", "text", false, UpdateOther(_)),
        button(`class` := "form-submit-btn", `type` := "button", onClick(AttemptPostJob))(
          s"Post job - ${Constants.jobAdvertPrice}"
        )
      )

  //////////////////////////////////////////
  // private
  //////////////////////////////////////////
  private def parseNumber(s: String) =
    Try(s.toInt).toOption

  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))

  private def renderInvalidContents() =
    List(p(`class` := "form-text")("You need to be logged in to view this page"))


object PostJobPage:
  trait Msg                                           extends Page.Msg
  case class UpdateCompany(v: String)                 extends Msg
  case class UpdateTitle(v: String)                   extends Msg
  case class UpdateDescription(v: String)             extends Msg
  case class UpdateExternalUrl(v: String)             extends Msg
  case object ToggleRemote                            extends Msg
  case class UpdateLocation(v: String)                extends Msg
  case class UpdateSalaryLo(v: Option[Int])           extends Msg
  case class UpdateSalaryHi(v: Option[Int])           extends Msg
  case class UpdateCurrency(v: String)                extends Msg
  case class UpdateCountry(v: String)                 extends Msg
  case class UpdateImageFile(maybeFile: Option[File]) extends Msg
  case class UpdateImage(maybeImage: Option[String])  extends Msg
  case class UpdateSeniority(v: String)               extends Msg
  case class UpdateOther(v: String)                   extends Msg
  case class UpdateTags(v: String)                    extends Msg
  case class PostJobSuccess(jobId: String)            extends Msg
  case class PostJobError(error: String)              extends Msg
  // actions
  case object AttemptPostJob extends Msg

  object Endpoints:
    val postJob = new Endpoint[Page.Msg | Router.Msg]:
      override val location: String = Constants.endpoints.postJob
      override val method: Method   = Method.Post
      override val onError: HttpError => Page.Msg | Router.Msg =
        e => PostJobError(e.toString)
      override val onResponse: Response => Page.Msg | Router.Msg =
        Endpoint.onResponseText(PostJobSuccess(_), PostJobError(_))

    val postJobPromoted = new Endpoint[Page.Msg | Router.Msg]:
      override val location: String                            = Constants.endpoints.postJobPromoted
      override val method: Method                              = Method.Post
      override val onError: HttpError => Page.Msg | Router.Msg = e => PostJobError(e.toString)
      override val onResponse: Response => Page.Msg | Router.Msg =
        Endpoint.onResponseText(Router.ExternalRedirect(_), PostJobError(_))

  object Commands:
    def postJob(promoted: Boolean = true)(
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
    ): Cmd[IO, Page.Msg | Router.Msg] =
      val endpoint =
        if (promoted) Endpoints.postJobPromoted
        else Endpoints.postJob

      endpoint.callAuthorized(
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

    def loadFileBasic(maybeFile: Option[File]) =
      Cmd.Run[IO, Option[String], Msg](
        maybeFile.traverse: file =>
          IO.async_ : cb =>
            val reader = new FileReader
            reader.onload = _ => cb(Right(reader.result.toString))
            reader.readAsDataURL(file)
      )(UpdateImage(_))

    def loadFile(maybeFile: Option[File]) =
      Cmd.Run[IO, Option[String], Msg](
        maybeFile.traverse: file =>
          IO.async_ : cb =>
            val reader = new FileReader
            reader.onload = _ =>
              val img = document.createElement("img").asInstanceOf[HTMLImageElement]
              img.addEventListener(
                "load",
                _ =>
                  // resize to 256x256 (some backend limits could be useful too, in the future)
                  val canvas  = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
                  val context = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

                  val (width, height) = computeDimensions(img.width, img.height)
                  canvas.width = width
                  canvas.height = height
                  context.drawImage(img, 0, 0, canvas.width, canvas.height)
                  cb(Right(canvas.toDataURL(file.`type`)))
              )
              img.src = reader.result.toString
            reader.readAsDataURL(file)
      )(UpdateImage(_))

    private def computeDimensions(w: Int, h: Int): (Int, Int) =
      if (w > h)
        val ratio = w * 1.0 / 256
        val w1    = w / ratio
        val h1    = h / ratio
        (w1.toInt, h1.toInt)
      else
        val (h1, w1) = computeDimensions(h, w)
        (w1, h1)
