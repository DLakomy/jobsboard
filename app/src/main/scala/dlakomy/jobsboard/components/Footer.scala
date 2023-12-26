package dlakomy.jobsboard.components

import dlakomy.jobsboard.*
import tyrian.Html.*
import tyrian.*

import scala.scalajs.js.Date


object Footer:
  def view(): Html[App.Msg] =
    div(`class` := "footer")(
      p(
        text("Written in "),
        a(href := "https://scala-lang.org", target := "blank")("Scala"),
        text(" with ❤️ at "),
        a(href := "https://rockthejvm.com", target := "blank")("Rock the JVM")
      ),
      p(s"© Rock the JVM ${new Date().getFullYear()}, don't copy me")
    )
