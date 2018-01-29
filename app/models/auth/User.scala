package aurita.models.auth

import aurita.models.utility.Common

/** Provide a user with a given [[UserRole role]].
 *
 * Data model for the [[titusi.daos.auth.UserDAOInterface.UserDAOImpl.tableQuery user]]
 * database table.
 *
 * @constructor create a new user.
 * @param id the primary key.
 * @param groupId the foreign key to the user's primary [[UserGroup group]].
 * @param roleId the foreign key to the [[UserRole role]] of the user.
 * @param statusId the foreign key to the user's account
 * [[aurita.models.CurrentStatus status]].
 * @param userId the id from the login service provider.
 * @param avatarURL the URL to the user's avatar.
 * @param email the user's email address.
 * @param firstName the user's first name.
 * @param lastName the user's last name.
 * @param username the user's username.
 * @param usernameSuffix suffix that is added to username to form a unique
 * username.
 */
case class User(
  id: Option[Long],
  groupId: Long,
  roleId: Long,
  statusId: Long,
  userId: String,
  avatarURL: Option[String],
  email: String,
  firstName: Option[String],
  lastName: Option[String],
  username: String,
  usernameSuffix: Option[Int]
) extends Common[User] {
  def fullName: Option[String] = (
    firstName ++ lastName
  ).reduceOption(_ + " " + _)

  def fullUsername: String = username + usernameSuffix.getOrElse("")
}

/**
 * A representation of [[User user]] with readable values.
 *
 * @param id the primary key.
 * @param groupName the name of the user's [[UserGroup group]].
 * @param roleName the name of the user's [[UserRole role]].
 * @param statusValue the value of the user's account
 * [[aurita.models.CurrentStatus status]].
 * @param userId the id from the login service provider.
 * @param avatarURL the URL to the user's avatar.
 * @param email the user's email address.
 * @param fullName the user's full name.
 * @param firstName the user's first name.
 * @param lastName the user's last name.
 * @param username the user's username.
 * @param usernameSuffix suffix that is added to username to form a unique
 * username.
 */
case class UserReadable(
  id: Option[Long],
  groupName: String,
  roleName: String,
  statusValue: String,
  userId: String,
  avatarURL: Option[String],
  email: String,
  firstName: Option[String],
  lastName: Option[String],
  username: String,
  usernameSuffix: Option[Int]
) {
  def fullName: Option[String] = (
    firstName ++ lastName
  ).reduceOption(_ + " " + _)
}

/**
 * The companion object.
 */
object UserReadable {
  import play.api.libs.json.{ Json, OFormat }

  implicit lazy val jsonFormat: OFormat[UserReadable] = Json.format[UserReadable]
}
