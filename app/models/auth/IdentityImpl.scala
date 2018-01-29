package aurita.models.auth

import com.mohiva.play.silhouette
import silhouette.api.Identity

/**
 * Impl of silhouette's [[com.mohiva.play.silhouette.api.Identity authenticated user object.]]
 *
 * @param user the [[UserReadable user]] object in readable format.
 * @param loginInfo the [[silhouette.api.LoginInfo login profile]]
 * of the user.
 *
 */
case class IdentityImpl(
  user: UserReadable, loginInfo: silhouette.api.LoginInfo
) extends Identity

