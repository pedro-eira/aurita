package aurita.models.auth

import aurita.models.utility.Common

/**
 * The password details.
 *
 * @param id the primary key
 * @param loginInfoId the forein key to the user's login info.
 * @param hasher The id of the hasher used to hash this password.
 * @param password The hashed password.
 * @param salt The optional salt used when hashing.
 */
case class PasswordInfo(
  id: Option[Long],
  loginInfoId: Long,
  hasher: String,
  password: String,
  salt: Option[String]
) extends Common[PasswordInfo] {}
