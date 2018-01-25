package aurita.models.auth

import aurita.models.utility.Common

/** Provides a [[https://wiki.archlinux.org/index.php/Users_and_groups grouping of users]],
  * for priviledged access to resources.
  *
  * Data model for the
  * [[aurita.daos.auth.UserGroupDAOInterface.UserGroupDAOImpl.tableQuery user group]]
  * database table.
  *
  * @constructor create a new group.
  * @param id the primary key
  * @param active whether the group is available
  * @param description the description of the group
  * @param name the name of the group
  */
case class UserGroup(
  id: Option[Long],
  active: Boolean,
  description: String,
  name: String
) extends Common[UserGroup] {}
