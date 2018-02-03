package aurita.controllers.auth.forms

/**
 * The form which handles the sign up process.
 */
object SignUpForm {
  import play.api.data.Form
  import play.api.data.Forms._
  import play.api.libs.json.Json

  val form = Form(
    mapping(
      "username" -> nonEmptyText,
      "firstName" -> optional(nonEmptyText),
      "lastName" -> optional(nonEmptyText),
      "email" -> email,
      "password" -> nonEmptyText,
      "confirmPassword" -> nonEmptyText
    )(SignUpData.apply)(SignUpData.unapply)
  )

  /**
   * The Sign-Up form data.
   *
   * @param username The screen name of a user.
   * @param firstName The first name of a user.
   * @param lastName The last name of a user.
   * @param email The email of the user.
   * @param password The password of the user.
   */
  case class SignUpData(
    username: String,
    firstName: Option[String],
    lastName: Option[String],
    email: String,
    password: String,
    confirmPassword: String
  )

  /**
   * The companion Sign-Up object.
   */
  object SignUpData {

    /**
     * Converts the [SignUpData] object to Json and vice versa.
     */
    implicit val jsonFormat = Json.format[SignUpData]
  }
}