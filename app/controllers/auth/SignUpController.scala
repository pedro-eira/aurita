package aurita.controllers.auth

import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import play.api.Configuration
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ AbstractController, ControllerComponents }
import aurita.utility.auth.DefaultEnv
import aurita.utility.mail.Mailer
import aurita.daos.auth.UserService
import scala.concurrent.ExecutionContext
import aurita.controllers.auth.daos.AuthControllerDAO

/**
 * The `Sign Up` controller.
 *
 * @param messagesApi the Play messages API.
 * @param silhouette the Silhouette stack.
 * @param userService the service to retrieve and save [[aurita.models.auth.User user]]
 * credentials.
 * @param authInfoRepository the object that persist authentication info.
 * @param avatarService the service that handles a [[aurita.models.auth.User user]]'s avatar.
 * @param passwordHasher the hasher for a [[aurita.models.auth.User user]]'s password.
 * @param controllerDAO the object giving access to other authentication related DAOs.
 */
class SignUpController(
  messagesApi: MessagesApi,
  mailer: Mailer,
  cc: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  controllerDAO: AuthControllerDAO
)(
  implicit executionContext: ExecutionContext, configuration: Configuration
) extends AbstractController(cc) with I18nSupport {
  import play.api.mvc.{ Action, Request, RequestHeader, Result }
  import scala.concurrent.Future
  import java.util.UUID
  import play.api.libs.json.{ Json, JsValue }
  import com.mohiva.play.silhouette.api.{ LoginEvent, LoginInfo, SignUpEvent }
  import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
  import java.sql.Timestamp
  import org.joda.time.DateTime
  import aurita.controllers.auth.forms.SignUpForm.SignUpData
  import aurita.models.auth.{ IdentityImpl, User, UserXGroup, UserReadable, UserToken }
  import aurita.utility.strings.FormatString.formatString
  import aurita.daos.auth.UserDAO.{
    BadDomainNameException,
    UnknownDomainNameException,
    DuplicateEmailException,
    DuplicateUsernameException
  }
  lazy val logger = new aurita.utility.messages.Log(this.getClass)

  /**
   * Handles the submitted sign-up JSON data.
   *
   * @return the result of the sign-up process.
   */
  def submit: Action[JsValue] = Action.async(parse.json) {
    implicit request  => request.body.validate[SignUpData].fold(
      errors => Future {
        Conflict(
          Json.obj("type" -> "signUp", "error" -> s"Error in sign-up data: ${errors}")
        )
      },
      data => controllerDAO.userDAO.isValidUsernameOrEmail(
        username = data.username, email = data.email
      ) flatMap {
        isValid: Boolean => _processSubmitData(data = data, isValid = isValid)
      } recover { case err => _processSubmitError(error = err) }
    )
  }

  private def _processSubmitData(
    data: SignUpData, isValid: Boolean
  )(implicit request: Request[_]): Future[Result] = {
    logger.debug(
      s"User signing up: ${data.username}, ${data.email}, ${CredentialsProvider.ID}"
    )
    val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
    userService.retrieve(loginInfo).flatMap {
      case Some(identity) => Future { Conflict(Json.obj(
        "type" -> "email", "error" -> "User exists", "mesg" -> ""
      )) }
      case None => (data.password == data.confirmPassword) match {
        case false => Future { Conflict(
          Json.obj(
            "type" -> "confirmPassword", "error" -> "Passwords do not match", "mesg" -> ""
          )
        ) }
        case true => _addUser(data = data, loginInfo = loginInfo)
      }
    }
  }

  private def _getUserReadable(data: SignUpData): UserReadable = UserReadable(
    id = None,
    groupName = data.username,
    roleName = "user",
    statusValue = "emailNotConfirmed",
    userId = UUID.randomUUID.toString,
    avatarURL = None,
    email = data.email,
    firstName = data.firstName,
    lastName = data.lastName,
    username = data.username,
    usernameSuffix = None
  )

  private def _addUser(
    data: SignUpData, loginInfo: LoginInfo
  )(implicit request: Request[_]): Future[Result] = {
    val passwordInfo = passwordHasher.hash(data.password)
    val user = _getUserReadable(data = data)
    val identityImpl: IdentityImpl = IdentityImpl(user = user, loginInfo = loginInfo)
    val f = for {
      avatar <- avatarService.retrieveURL(data.email)
      userXLoginInfo <- userService.insert(identityImpl.copy(
        user = identityImpl.user.copy(avatarURL = avatar)
      ))
      updatedPasswordInfo <- authInfoRepository.add(
        loginInfo = loginInfo, authInfo = passwordInfo
      )
      authenticator <- silhouette.env.authenticatorService.create(loginInfo)
      verifyToken <- controllerDAO.userTokenDAO.insert(
        UserToken(
          id = None,
          userId = userXLoginInfo.userId,
          tokenId = UUID.randomUUID.toString,
          expiresOn = _getNewExpiration
        )
      )
    } yield (verifyToken, updatedPasswordInfo)
    f map {
      case (token, _) => {
        mailer.welcome(
          email = user.email,
          name = user.fullName.getOrElse(user.username),
          link = aurita.controllers.auth.routes.SignUpController.verifyToken(
            token.tokenId
          ).absoluteURL()
        )
        Ok(Json.obj("some" -> "yo"))
      }
    } recover { case err => Conflict(Json.obj(
      "type" -> "email",
      "error" -> s"Unable to process sign up request. Please try again later: ${err}",
      "mesg" -> ""
    )) }
  }

  def verifyToken(tokenId: String) = Action.async { implicit request => {
    controllerDAO.userTokenDAO.findByTokenId(tokenId).flatMap {
      tokenOption: Option[UserToken] => tokenOption match {
        case Some(token) => _processFoundToken(token = token)
        case None => Future { Ok(
          views.html.auth.signUp.signUpStatus(
            error = Option("Invalid sign-up verification link.")
          )
        ) }
      }
    } recover { case err => Ok(views.html.auth.signUp.signUpStatus(
      error = Option(
        "Error processing sign-up verification link. Please contact support: ${err}"
      )
    )) }
  } }

  private def _processFoundToken(token: UserToken)(
    implicit request: Request[_]
  ): Future[Result] = {
    controllerDAO.userDAO.findUserReadableById(id = token.userId).flatMap {
      userOption: Option[UserReadable] => _processFoundUser(
        userOption = userOption, token = token
      )
    } recover { case err => Ok(views.html.auth.signUp.signUpStatus(
      error = Option(
        s"""Failure finding user associated with sign-up verification
          | link: ${err}""".tcombine,
      )
    )) }
  }

  private def _processFoundUser(userOption: Option[UserReadable], token: UserToken)(
    implicit request: Request[_]
  ): Future[Result] = userOption match {
    case Some(user) if (
      (user.statusValue == "emailNotConfirmed") && (!token.isExpired)
    ) => _setEmailConfirmed(token = token, user = user)
    case Some(user) if (user.statusValue == "emailConfirmed") => {
      controllerDAO.userTokenDAO.deleteByTokenId(token.tokenId) flatMap {
        _ => {
          val name = user.fullName.getOrElse(user.username)
          Future { Ok(views.html.auth.signUp.signUpStatus(
            name = Some(name), expired = Some(false)
          )) }
        }
      } recover { case err => {
        logger.error(s"Error deleting token ${token}: ${err}")
        val name = user.fullName.getOrElse(user.username)
        Ok(views.html.auth.signUp.signUpStatus(
          name = Some(name), expired = Some(false)
        ))
      } }
    }
    case Some(user) if (token.isExpired) => _processExpiredToken(
      token = token, user = user
    )
    case None => Future {
      Ok(views.html.auth.signUp.signUpStatus(
        error = Option(
          """Unable to find user associated with sign-up verification link.
           | Please contact support.""".tcombine
        )
      ))
    }
  }

  private def _processExpiredToken(token: UserToken, user: UserReadable)(
    implicit request: Request[_]
  ): Future[Result] = controllerDAO.userTokenDAO.upsert(
    token.copy(tokenId = UUID.randomUUID().toString, expiresOn = _getNewExpiration)
  ) flatMap {
    newToken => {
      val name = user.fullName.getOrElse(user.username)
      mailer.welcome(
        email = user.email,
        name = name,
        link = aurita.controllers.auth.routes.SignUpController.verifyToken(
          tokenId = newToken.tokenId
        ).absoluteURL()
      )
      Future { Ok(views.html.auth.signUp.signUpStatus(
        name = Some(name), expired = Some(true)
      )) }
    } recover { case err => Ok(views.html.auth.signUp.signUpStatus(
       error = Option(
         s"""Expired sign-up verification link: Error renewing link.
           | Please contact support: ${err}"""
       )
    )) }
  }

  private def _setEmailConfirmed(
    token: UserToken, user: UserReadable
  )(implicit request: RequestHeader): Future[Result] = {
    controllerDAO.userDAO.updateStatus(
      id = token.userId, statusValue = "emailConfirmed"
    ) flatMap {
      case _ => {
        val loginInfo: LoginInfo = LoginInfo(CredentialsProvider.ID, user.email)
        val identity: IdentityImpl = IdentityImpl(user = user, loginInfo = loginInfo)
        silhouette.env.eventBus.publish(SignUpEvent(identity, request))
        _deleteTokenAndRespond(tokenId = token.tokenId, user = user)
      }
    } recover { case err => Ok(views.html.auth.signUp.signUpStatus(
      error = Option(
        s"Error updating sign-up status. Please contact support: ${err}."
      )
    )) }
  }

  private def _deleteTokenAndRespond(
    tokenId: String, user: UserReadable
  )(implicit request: RequestHeader): Future[Result] = {
    val name = user.fullName.getOrElse(user.username)
    controllerDAO.userTokenDAO.deleteByTokenId(tokenId) flatMap {
      _ => Future { Ok(views.html.auth.signUp.signUpStatus(
        name = Some(name), expired = Some(false)
      )) }
    } recover { case err => {
      logger.error(s"Error deleting token: ${tokenId} of user ${user}: ${err}")
      Ok(views.html.auth.signUp.signUpStatus(
        name = Some(name), expired = Some(false)
      ))
    } }
  }

  private def _processSubmitError(error: Throwable): Result = error match {
    case e: DuplicateUsernameException => Conflict(Json.obj(
      "type" -> "username", "error" -> "Username already used", "mesg" -> ""
    ))
    case e: DuplicateEmailException => Conflict(Json.obj(
      "type" -> "email", "error" -> "Email already used", "mesg" -> ""
    ))
    case e: BadDomainNameException => Conflict(Json.obj(
      "type" -> "email",
      "error" -> "Please check your email's domain name",
      "mesg" -> ""
    ))
    case e: UnknownDomainNameException => Conflict(Json.obj(
      "type" -> "email",
      "error" -> "Please check your email's domain name",
      "mesg" -> ""
    ))
    case e => Conflict {
      logger.error(s"${e}")
      Json.obj(
        "type" -> "username",
        "error" -> s"Error checking username or email: ${e}",
        "mesg" -> ""
      )
    }
  }

  private def _getNewExpiration: Timestamp =
    new Timestamp((new DateTime()).plusHours(24).getMillis)
}
