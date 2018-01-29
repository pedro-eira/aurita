package aurita.models.auth

import aurita.models.utility.Common

/**
 * Link to an [[https://oauth.net/1/ OAuth1]] account type.
 *
 * The OAuth1 info contains data about the OAuth1 provider that authenticated a user.
 *
 * @param id the primary key.
 * @param loginInfoId a foreign key to the associated [[aurita.models.auth.LoginInfo login profile.]]
 * @param secret the token shared-secret.
 * @param token the token identifier.
 *
 */
case class OAuth1Info(
  id: Option[Long], loginInfoId: Long, secret: String, token: String
) extends Common[OAuth1Info] {}
