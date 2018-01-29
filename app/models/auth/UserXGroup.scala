package aurita.models.auth

import aurita.models.utility.Common

/** Provides a link between a [[User user]] and the [[UserGroup group(s)]]
  * they belong to.
  *
  * Data model for the
  * [[aurita.daos.auth.UserXGroupDAOInterface.UserXGroupDAOImpl.tableQuery user group]]
  * database table.
  *
  * @constructor create a new user group.
  * @param id the primary key
  * @param groupId the foreign key to the user's [[UserGroup group]]
  * @param userId the foreign key to the [[User user]]
  * @param userPermissionId the foreign key to the user's
  * [[UserPermission permission]]
  */
case class UserXGroup(
  id: Option[Long],
  groupId: Long,
  userId: Long,
  userPermissionId: Long
) extends Common[UserXGroup] {}
