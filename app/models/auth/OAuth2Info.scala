package aurita.models.auth

import aurita.models.utility.Common

/**
 * Link to an [[https://oauth.net/2/ OAuth2]] account type.
 *
 * The OAuth2 info contains data about the OAuth2 provider that authenticated a user.
 *
 * @param id the primary key.
 * @param loginInfoId a foreign key to the associated [[aurita.models.auth.LoginInfo login profile.]]
 * @param accessToken the credentials used to access protected resources (i.e. a string
 * representing an authorization issued to the client).
 * @param expiresIn the duration before the token expires.
 * @param refreshToken the credentials used to obtain access tokens.
 * @param tokenType the access token type from the provider.
 *
 */
case class OAuth2Info(
  id: Option[Long],
  loginInfoId: Long,
  accessToken: String,
  expiresIn: Option[Int],
  refreshToken: Option[String],
  tokenType: Option[String]
) extends Common[OAuth2Info] {}
