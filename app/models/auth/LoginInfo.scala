package aurita.models.auth

import aurita.models.utility.Common

/**
 * Links user to an login provider account (e.g. local, Facebook, Google
 * account etcetera).
 *
 * The login info contains data about the provider that authenticated a user.
 *
 * @param id the primary key.
 * @param providerId the identity of the provider.
 * @param providerKey a unique key which identifies the user on this provider.
 *
 */
case class LoginInfo(
  id: Option[Long],
  providerId: String,
  providerKey: String
) extends Common[LoginInfo] {}
