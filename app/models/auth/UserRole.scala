package aurita.models.auth

import aurita.models.utility.Common

/** Provide the role that determines the user's privileges.
  *
  * Data model for the
  * [[aurita.daos.auth.UserRoleDAOInterface.UserRoleDAOImpl.tableQuery user role]]
  * database table.
  *
  * @constructor create a new role.
  * @param id the primary key
  * @param active whether the role is available
  * @param description the description of the role
  * @param name the name of the role
  * @param selectable whether the role is available for selection
  * by the user
  */
case class UserRole(
  id: Option[Long],
  active: Boolean,
  description: String,
  name: String,
  selectable: Boolean
) extends Common[UserRole] {}
