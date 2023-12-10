package dlakomy.jobsboard

import org.scalajs.dom.document

import scala.scalajs.js.annotation.*


@JSExportTopLevel("JobsboardApp")
class App:
  @JSExport
  def doSomething(containerId: String) =
    document.getElementById(containerId).innerHTML = "It's working!"
