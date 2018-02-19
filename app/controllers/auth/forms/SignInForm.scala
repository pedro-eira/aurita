package aurita.controllers.auth.forms

/**
 * The form which handles the sign in process.
 */
object SignInForm {
  import play.api.data.Form
  import play.api.data.Forms._
  import play.api.libs.json.Json

  val form = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText,
      "rememberMe" -> boolean
    )(SignInData.apply)(SignInData.unapply)
  )

  /**
   * The Sign-In form data.
   *
   * @param email The email of the user.
   * @param password The password of the user.
   * @param rememberMe Indicates if the user should stay logged in on the next visit.
   */
  case class SignInData(
    email: String,
    password: String,
    rememberMe: Boolean
  )

  /**
   * The companion Sign-In object.
   */
  object SignInData {

    /**
     * Converts the [SignInData] object to Json and vice versa.
     */
    implicit val jsonFormat = Json.format[SignInData]
  }
}
