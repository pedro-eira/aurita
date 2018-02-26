package aurita.controllers.auth.forms

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * The form which handles the reset password process.
 */
object SaveNewPasswordForm {
  import play.api.data.Form
  import play.api.data.Forms._
  import play.api.libs.json.Json

  val form = Form(
    mapping(
      "password" -> nonEmptyText,
      "confirmPassword" -> nonEmptyText
    )(SaveNewPasswordData.apply)(SaveNewPasswordData.unapply)
  )

  /**
   * The Save New Password form data.
   *
   * @param password The password of the user.
   * @param confirmPassword The confirmation of the password.
   */
  case class SaveNewPasswordData(password: String, confirmPassword: String)

  /**
   * The companion Save New Password object.
   */
  object SaveNewPasswordData {

    /**
     * Converts the [SaveNewPasswordData] object to Json and vice versa.
     */
    implicit val jsonFormat = Json.format[SaveNewPasswordData]
  }
}
