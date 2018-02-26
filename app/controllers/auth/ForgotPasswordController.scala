package aurita.controllers.auth

import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ AbstractController, ControllerComponents }
import play.api.Configuration
import aurita.utility.mail.Mailer
import aurita.daos.auth.UserService
import scala.concurrent.ExecutionContext
import aurita.controllers.auth.daos.AuthControllerDAO
import play.api.mvc.Action
  
/**
 * The `Forgot Password` controller.
 *
 * @param controllerDAO the object giving access to other authentication related DAOs.
 * @param messagesApi the Play messages API.
 * @param the [[https://github.com/playframework/play-mailer Play Mailer]] service used to
 * send emails
 * @param userService the service to retrieve and save [[aurita.models.auth.User user]]
 * credentials.
 */
class ForgotPasswordController(
  authInfoRepository: AuthInfoRepository,
  cc: ControllerComponents,
  controllerDAO: AuthControllerDAO,
  messagesApi: MessagesApi,
  mailer: Mailer,
  passwordHasher: PasswordHasher,
  userService: UserService
)(
  implicit executionContext: ExecutionContext, configuration: Configuration
) extends AbstractController(cc) with I18nSupport {
  import java.sql.Timestamp
  import java.util.UUID
  import org.joda.time.DateTime
  import play.api.mvc.{ Request, RequestHeader, Result }
  import scala.concurrent.Future
  import play.api.libs.json.{ Json, JsValue }
  import aurita.models.auth.{ User, UserReadable, UserToken }
  import aurita.models.auth.utility.ResetPasswordData
  import aurita.controllers.auth.forms.ForgotPasswordForm.ForgotPasswordData
  import aurita.controllers.auth.forms.SaveNewPasswordForm.SaveNewPasswordData
  import aurita.utility.strings.FormatString.formatString

  lazy val logger = new aurita.utility.messages.Log(this.getClass)

  /**
   * Handles the submitted forgot password JSON data.
   *
   * @return the result of the forgot password process.
   */
  def forgotPassword: Action[JsValue] = Action.async(parse.json) {
    implicit request  => request.body.validate[ForgotPasswordData].fold(
      errors => Future { Unauthorized(Json.obj(
        "type" -> "other", "mesg" -> "Invalid password data", "error" -> s"${errors}"
      )) },
      data => controllerDAO.userDAO.findByEmail(data.email) flatMap {
        user: Option[User] => _processFPSubmitData(userOption = user)
      } recover { case err: Throwable => Conflict {
        logger.error(s"Error processing forgot password: ${err}")
        Json.obj(
          "type" -> "forgotPassword",
          "error" -> s"Error processing forgot password. Please contact support: ${err}",
          "mesg" -> ""
        )
      } }
    )
  }

  private def _processFPSubmitData(
    userOption: Option[User]
  )(implicit request: Request[_]): Future[Result] = userOption match {
    case Some(user) => controllerDAO.userTokenDAO.findByUserId(user.id.get) flatMap {
      token: Option[UserToken] => _processFPToken(tokenOption = token, user = user)
    } recover {
      case err => Conflict(Json.obj(
        "type" -> "other",
        "error" -> s"Forgot password failure. Please contact support: ${err}",
        "mesg" -> ""
      ))
    }
    case None => Future { Conflict(Json.obj(
      "type" -> "email", "error" -> "Email does not exist.", "mesg" -> ""
    )) }
  }

  private def _processFPToken(
    tokenOption: Option[UserToken], user: User
  )(implicit request: Request[_]): Future[Result] = tokenOption match {
    case Some(token) if !token.isExpired => {
      mailer.forgotPassword(
        email = user.email,
        name = user.fullName.getOrElse(user.username),
        link = aurita.controllers.auth.routes.ForgotPasswordController.resetPassword(
          tokenId = token.tokenId, userId = token.userId
        ).absoluteURL()
      )
      Future { Ok(Json.obj(
        "type" -> "forgotPassword",
        "fullName" -> user.fullName
      )) }
    }
    case Some(token) => _sendForgotPasswordVerifyEmail(
      user = user, token = token.copy(
        tokenId = UUID.randomUUID().toString,
        expiresOn = _getNewExpiration
      )
    )
    case None => _sendForgotPasswordVerifyEmail(
      user = user, UserToken(
        id = None,
        tokenId = UUID.randomUUID().toString,
        userId = user.id.get,
        expiresOn = _getNewExpiration
      )
    )
  }

  private def _getNewExpiration: Timestamp =
    new Timestamp((new DateTime()).plusHours(1).getMillis)

  private def _sendForgotPasswordVerifyEmail(user: User, token: UserToken)(
    implicit request: RequestHeader
  ): Future[Result] = controllerDAO.userTokenDAO.upsert(token = token) flatMap {
    forgotPasswordToken: UserToken => Future {
      mailer.forgotPassword(
        email = user.email,
        name = user.fullName.getOrElse(user.username),
        link = aurita.controllers.auth.routes.ForgotPasswordController.resetPassword(
          tokenId = forgotPasswordToken.tokenId, userId = forgotPasswordToken.userId
        ).absoluteURL()
      )
      Ok(Json.obj("type" -> "forgotPassword", "fullName" -> user.fullName))
    }
  } recover {
    case err => Conflict(Json.obj(
      "type" -> "other",
      "mesg" -> s"Error generating reset password link. Please contact support: ${err}",
      "error" -> ""
    ))
  }

  def resetPassword(tokenId: String, userId: Long) = Action.async { implicit request =>
    controllerDAO.userTokenDAO.findByTokenId(tokenId).flatMap {
      token: Option[UserToken] => _processRPToken(tokenOption = token, userId = userId)
    } recover {
      case err => Conflict(Json.obj(
        "type" -> "other",
        "error" -> s"Error in reset password link. Please contact support: ${err}.",
        "mesg" -> ""
      ))
    }
  }

  private def _processRPToken(
    tokenOption: Option[UserToken], userId: Long
  )(implicit request: Request[_]): Future[Result] = tokenOption match {
    case Some(token) => (token.userId == userId) match {
      case false => Future { Conflict(Json.obj(
        "type" -> "other",
        "erro" -> "User does not have access to link.",
        "mesg" -> ""
      )) }
      case true => controllerDAO.userDAO.findUserReadableById(token.userId) flatMap {
        user: Option[UserReadable] => _processRPUser(token = token, userOption = user)
      } recover {
        case err => Conflict(Json.obj(
          "type" -> "other",
          "erro" -> "Failure while finding user. Please contact support: ${err}",
          "mesg" -> ""
        ))
      }
    }
    case None => Future { Conflict(Json.obj(
      "type" -> "other", "error" -> "Unable to process link.", "mesg" -> ""
    )) }
  }

  private def _processRPUser(
    token: UserToken, userOption: Option[UserReadable]
  )(implicit request: Request[_]): Future[Result] = userOption match {
    case Some(user) if (
      (user.statusValue == "emailNotConfirmed") && (!token.isExpired)
    ) => controllerDAO.userDAO.updateStatus(token.userId, "emailConfirmed") flatMap {
      _ => Future { Ok(views.html.auth.forgotPassword.resetPassword()) }
    } recover {
      case err => Conflict(Json.obj(
        "type" -> "other",
        "error" -> "Error updating user status. Please contact support: ${err}",
        "mesg" -> ""
      ))
    }
    case Some(user) if (!token.isExpired) => Future {
      Ok(views.html.auth.forgotPassword.resetPassword())
    }
    case Some(user) => _processExpiredToken(token: UserToken, user: UserReadable)
    case None => Future { Conflict(Json.obj(
      "type" -> "other",
      "error" -> "Unable to find user. Please contact support.",
      "mesg" -> ""
    )) }
  }

  private def _processExpiredToken(
    token: UserToken, user: UserReadable
  )(implicit request: Request[_]): Future[Result] = controllerDAO.userTokenDAO.upsert(
    token.copy(tokenId = UUID.randomUUID().toString, expiresOn = _getNewExpiration)
  ) flatMap {
    newToken: UserToken => {
      mailer.forgotPassword(
        email = user.email,
        name = user.fullName.getOrElse(user.username),
        link = aurita.controllers.auth.routes.ForgotPasswordController.resetPassword(
          tokenId = newToken.tokenId, userId = newToken.userId
        ).absoluteURL()
      )
      Future {
        Ok(views.html.auth.forgotPassword.resetPasswordExpiredToken(user))
      }
    } recover {
      case err => Conflict(Json.obj(
        "type" -> "other",
        "error" -> "Error generating reset password link. Please contact support: ${err}",
        "mesg" -> ""
      ))
    }
  }

  /**
   * Handles the submitted save new password JSON data.
   *
   * @return the result of the save new password process.
   */
  def saveNewPassword(
    userId: Long, tokenId: String
  ): Action[JsValue] = Action.async(parse.json) {
    implicit request  => request.body.validate[SaveNewPasswordData].fold(
      errors => Future { Unauthorized(Json.obj(
        "type" -> "other", "mesg" -> "Invalid password data", "error" -> s"${errors}"
      )) },
      data => controllerDAO.userTokenDAO.getResetPasswordDataByUserId(userId) flatMap {
        rpd: Option[ResetPasswordData] => _processSNPSubmitData(
          data = data, rpdOption = rpd, tokenId: String
        )
      } recover { case err: Throwable => Conflict {
        logger.error(s"Error processing reset password: ${err}")
        Json.obj(
          "type" -> "other",
          "error" -> s"Error saving new password. Please contact support: ${err}",
          "mesg" -> ""
        )
      } }
    )
  }

  private def _processSNPSubmitData(
    data: SaveNewPasswordData, rpdOption: Option[ResetPasswordData], tokenId: String 
  )(implicit request: Request[_]): Future[Result] = rpdOption match {
    case Some(rpd) if (rpd.token.tokenId != tokenId) => Future { Conflict(Json.obj(
      "type" -> "other",
      "error" -> "Reset password link's token don't match.",
      "mesg" -> ""
    )) }
    case Some(rpd) if (!rpd.token.isExpired) => _processSNPData(data = data, rpd = rpd)
    case Some(rpd) => _processSNPExpiredToken(data = data, rpd = rpd)
    case None => Future { Conflict(Json.obj(
      "type" -> "other",
      "error" -> "Unknown reset password link. Please contact support.",
      "mesg" -> ""
    )) }
  }

  private def _processSNPData(
    data: SaveNewPasswordData, rpd: ResetPasswordData 
  )(implicit request: Request[_]): Future[Result] = (
    data.password == data.confirmPassword
  ) match {
    case false => Future { Conflict(Json.obj(
      "type" -> "confirmPassword", "error" -> "Passwords do not match", "mesg" -> ""
    )) }
    case true => {
      val passwordInfo = passwordHasher.hash(data.password)
      authInfoRepository.save(rpd.loginInfo, passwordInfo) flatMap {
        _ => controllerDAO.userTokenDAO.deleteByTokenId(rpd.token.tokenId) flatMap {
          _ => Future { Ok(Json.obj(
            "type" -> "resetPassword", "fullName" -> rpd.user.fullName
          )) }
        } recover { case err => {
          logger.error(s"Failure deleting token for data ${rpd}: ${err}")
          Ok(Json.obj("type" -> "resetPassword", "fullName" -> rpd.user.fullName))
        } }
      } recover { case err => Conflict(Json.obj(
        "type" -> "other",
        "error" -> "Failure saving credentials. Please contact support: ${err}",
        "mesg" -> ""
      )) }
    }
  }

  private def _processSNPExpiredToken(
    data: SaveNewPasswordData, rpd: ResetPasswordData 
  )(implicit request: Request[_]): Future[Result] = controllerDAO.userTokenDAO.upsert(
    rpd.token.copy(tokenId = UUID.randomUUID().toString, expiresOn = _getNewExpiration)
  ) flatMap {
    newToken: UserToken  => {
      mailer.forgotPassword(
        email = rpd.user.email,
        name = rpd.user.fullName.getOrElse(rpd.user.username),
        link = aurita.controllers.auth.routes.ForgotPasswordController.resetPassword(
          tokenId = newToken.tokenId, userId = newToken.userId
        ).absoluteURL()
      )
      Future { Ok(Json.obj(
        "type" -> "other",
        "error" -> s"""Expired link. Check your mailbox for a new reset
          |password link we just mailed you.""".tcombine
      )) }
    }
  } recover {
    case err => Conflict(Json.obj(
      "type" -> "other",
      "error" -> "Failure generating reset password link. Please contact support: ${err}",
      "mesg" -> ""
    ))
  }

}
