package aurita.controllers.auth

import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import play.api.Configuration
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ AbstractController, ControllerComponents }
import aurita.utility.auth.DefaultEnv
import aurita.utility.mail.Mailer
import aurita.daos.auth.UserService
import scala.concurrent.ExecutionContext
import aurita.controllers.auth.daos.AuthControllerDAO
import play.api.mvc.Action
  
/**
 * The `Sign In` controller.
 *
 * @param clock The Silhouette clock instance.
 * @param configuration the Play configuration.
 * @param controllerDAO the object giving access to other authentication related DAOs.
 * @param credentialsProvider the Silhouette credentials provider.
 * @param messagesApi the Play messages API.
 * @param the [[https://github.com/playframework/play-mailer Play Mailer]] service used to
 * send emails
 * @param silhouette the Silhouette stack.
 * @param userService the service to retrieve and save [[aurita.models.auth.User user]]
 * credentials.
 */
class SignInController(
  cc: ControllerComponents,
  clock: Clock,
  controllerDAO: AuthControllerDAO,
  credentialsProvider: CredentialsProvider,
  messagesApi: MessagesApi,
  mailer: Mailer,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService
)(
  implicit executionContext: ExecutionContext, configuration: Configuration
) extends AbstractController(cc) with I18nSupport {
  import com.mohiva.play.silhouette.api.util.Credentials
  import org.joda.time.DateTime
  import play.api.mvc.{ Request, Result }
  import scala.concurrent.Future
  import play.api.libs.json.{ Json, JsValue }
  import com.mohiva.play.silhouette.api.{ LoginEvent, LoginInfo }
  import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
  import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
  import scala.concurrent.duration.FiniteDuration
  import aurita.controllers.auth.forms.SignInForm.SignInData
  import aurita.models.auth.IdentityImpl
  import aurita.utility.strings.FormatString.formatString

  lazy val logger = new aurita.utility.messages.Log(this.getClass)

  /**
   * Handles the submitted sign-in JSON data.
   *
   * @return the result of the sign-in process.
   */
  def submit: Action[JsValue] = Action.async(parse.json) {
    implicit request  => request.body.validate[SignInData].fold(
      errors => Future {
        logger.error(s"Invalid credentials: ${errors}")
        Unauthorized(Json.obj(
          "type" -> "other", "error" -> s"Invalid credentials: ${errors}", "mesg" -> ""
        ))
      },
      data => credentialsProvider.authenticate(
        Credentials(data.email, data.password)
      ) flatMap {
        loginInfo: LoginInfo => _processSubmitData(data = data, loginInfo = loginInfo)
      } recover { case err: Throwable => _processSubmitError(error = err) }
    )
  }

  private def _processSubmitData(
    data: SignInData, loginInfo: LoginInfo
  )(implicit request: Request[_]): Future[Result] = {
    logger.debug(s"User signing in: ${data.email}")
    userService.retrieve(loginInfo) flatMap {
      case Some(identity) if identity.user.statusValue == "emailNotConfirmed" => Future {
        Conflict(Json.obj(
          "type" -> "other",
          "mesg" -> "Access Denied",
          "error" -> s"""Email not confirmed. Please check your mailbox for your 
            |confirmation email, or click 'Reset it' below to reset password & 
            |reconfirm email.""".tcombine
        ))
      }
      case Some(identity) if identity.user.statusValue == "emailConfirmed" =>
        _processConfirmedEmail(
          identity = identity, loginInfo = loginInfo, rememberMe = data.rememberMe
        )
      case Some(identity) => Future { Conflict(Json.obj(
        "type" -> "other",
        "mesg" -> "Access Denied",
        "error" ->
          "User has status = ${identity.user.statusValue}. Please contact support."
      )) }
      case None => Future {
        Conflict(Json.obj("type" -> "email", "error" -> "Email not found", "mesg" -> ""))
      }
    }
  }

  private def _processConfirmedEmail(
    identity: IdentityImpl, loginInfo: LoginInfo, rememberMe: Boolean
  )(
    implicit request: Request[_]
  ): Future[Result] = silhouette.env.authenticatorService.create(loginInfo).map {
    case authenticator if rememberMe => authenticator.copy(
      expirationDateTime = _getExpiration,
      idleTimeout = configuration.getOptional[FiniteDuration](
        "silhouette.authenticator.rememberMe.authenticatorIdleTimeout"
      )
    )
    case authenticator => authenticator
  } flatMap { authenticator: DefaultEnv#A => _processAuth(
    authenticator = authenticator, identity = identity
  ) }

  private def _getExpiration: DateTime = clock.now.plus(
    configuration.getOptional[FiniteDuration](
      "silhouette.authenticator.rememberMe.authenticatorExpiry"
    ) match {
      case Some(dur) => dur.toMillis
      case None => 0
    }
  )

  private def _processAuth(authenticator: DefaultEnv#A, identity: IdentityImpl)(
    implicit request: Request[_]
  ): Future[Result] = {
    silhouette.env.eventBus.publish(LoginEvent(identity, request))
    silhouette.env.authenticatorService.init(authenticator).map { token =>
      Ok(Json.obj(
        "token" -> token,
        "id" -> identity.user.id,
        "fullName" -> identity.user.fullName,
        "username" -> identity.user.username,
        "role" -> identity.user.roleName
      ))
    }
  }

  private def _processSubmitError(error: Throwable): Result = error.toString match {
    case mesg if mesg matches "(?i).*PasswordException.*" => Conflict(Json.obj(
      "type" -> "password", "error" -> s"${error}", "mesg" -> "Incorrect password"
    ))
    case mesg if mesg matches "(?i).*IdentityNotFoundException.*" => Conflict(
      Json.obj("type" -> "email", "error" -> s"${error}", "mesg" -> "Email not found")
    )
    case mesg => Conflict(Json.obj(
      "type" -> "other", "mesg" -> "Access Denied", "error" -> s"${error}"
    ))
  }
}
