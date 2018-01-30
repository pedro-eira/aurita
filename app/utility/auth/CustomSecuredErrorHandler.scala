package aurita.utility.auth

import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import play.api.i18n.{ MessagesApi, I18nSupport, Messages }
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.Future

/**
 * Custom secured error handler.
 *
 * @param messagesApi The Play messages API.
 */
class CustomSecuredErrorHandler(val messagesApi: MessagesApi)
  extends SecuredErrorHandler with I18nSupport {
  import play.api.libs.json.Json

  /**
   * Called when a user is not authenticated.
   *
   * Return a JSON object indicating user should be redirected to the login page.
   *
   * @param request The request header.
   * @return The result to send to the client.
   */
  override def onNotAuthenticated(implicit request: RequestHeader) = {
    Future.successful(
      BadRequest(
        Json.obj("error" -> "User not authenticated", "login" -> true)
      )
    )
  }

  /**
   * Called when a user is authenticated but not authorized.
   *
   * Return a JSON object indicating user should be redirected to the intro page.
   *
   * @param request The request header.
   * @return The result to send to the client.
   */
  override def onNotAuthorized(implicit request: RequestHeader) = {
    Future.successful(
      BadRequest(
        Json.obj("error" -> "User not authorized", "intro" -> true)
      )
    )
  }
}
