package dlakomy.jobsboard.common

import org.scalajs.dom.window

import scala.scalajs.LinkingInfo
import scala.scalajs.js
import scala.scalajs.js.annotation.*


object Constants:
  @js.native
  @JSImport("/static/img/rtjvmlogo_128x128.png", JSImport.Default)
  val logoImage: String = js.native

  @js.native
  @JSImport("/static/img/resume.png", JSImport.Default)
  val jobImagePlaceholder: String = js.native

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""

  val defaultPageSize = 20
  val jobAdvertPrice  = "66 PLN"

  object endpoints:
    val root =
      if (LinkingInfo.developmentMode) "http://localhost:1234"
      else window.location.origin
    val signUp          = s"$root/api/auth/users"
    val login           = s"$root/api/auth/login"
    val logout          = s"$root/api/auth/logout"
    val checkToken      = s"$root/api/auth/checkToken"
    val forgotPassword  = s"$root/api/auth/reset"
    val resetPassword   = s"$root/api/auth/recover"
    val changePassword  = s"$root/api/auth/users/password"
    val postJob         = s"$root/api/jobs/create"
    val postJobPromoted = s"$root/api/jobs/promoted"
    val jobs            = s"$root/api/jobs"
    val filters         = s"$root/api/jobs/filters"

  object cookies:
    val duration = 10 * 24 * 3600 * 1000
    val email    = "email"
    val token    = "token"
