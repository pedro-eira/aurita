package aurita.models.auth

import aurita.models.utility.Common

/** Provide access permission for a [[User user]]
  *
  * Data model for the
  * [[aurita.daos.auth.UserPermissionDAOInterface.UserPermissionDAOImpl.tableQuery user permission]]
  * database table.
  *
  * @constructor create a new user permission.
  * @param id the primary key.
  * @param roleId the foreign key to the [[UserRole role]] of the user.
  * @param active whether this permission is available.
  * @param read whether the user can read data.
  * @param write whether the user can write data to files.
  * @param execute whether the user can execute files.
  */
case class UserPermission(
  id: Option[Long],
  roleId: Long,
  active: Boolean,
  read: Boolean,
  write: Boolean,
  execute: Boolean
) extends Common[UserPermission] {}
