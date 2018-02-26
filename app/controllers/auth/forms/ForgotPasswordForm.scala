package aurita.controllers.auth.forms

/**
 * The form which handles the forgot password process.
 */
object ForgotPasswordForm {
  import play.api.data.Form
  import play.api.data.Forms._
  import play.api.libs.json.Json

  val form = Form(
    mapping("email" -> email)(ForgotPasswordData.apply)(ForgotPasswordData.unapply)
  )

  /**
   * The Forgot Password form data.
   *
   * @param email The email of the user.
   */
  case class ForgotPasswordData(email: String)

  /**
   * The companion Forgot Password object.
   */
  object ForgotPasswordData {

    /**
     * Converts the [ForgotPasswordData] object to Json and vice versa.
     */
    implicit val jsonFormat = Json.format[ForgotPasswordData]
  }

}
