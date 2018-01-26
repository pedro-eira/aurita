package aurita.models.auth

import aurita.models.utility.Common

/**
 * Provides the [[LoginInfo login info]] associated with a [[User user]].
 *
 * @param id the primary key
 * @param loginInfoId the foreign key to the [[LoginInfo login info]] table.
 * @param userId the foreign key to the [[User user]] table.
 */
case class UserXLoginInfo(
  id: Option[Long], loginInfoId: Long, userId: Long
) extends Common[UserXLoginInfo] {}
