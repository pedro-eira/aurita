package aurita.models.auth.utility

import com.mohiva.play.silhouette
import aurita.models.auth.{ User, UserToken }

/** Container to hold data for resetting a user [[aurita.models.auth.PasswordInfo password]]
  *
  * @param loginInfo the [[silhouette.api.LoginInfo login profile]].
  * @param token the [[aurita.models.auth.UserToken token]] associated with the password reset.
  * @param user the [[aurita.models.auth.User user]] whose password needs to be reset.
  *
  */
case class ResetPasswordData(
  loginInfo: silhouette.api.LoginInfo, token: UserToken, user: User
)
