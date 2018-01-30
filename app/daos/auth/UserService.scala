package aurita.daos.auth

import java.util.UUID
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext
import aurita.models.auth.{ IdentityImpl, UserXLoginInfo, UserReadable }

/**
 * Handles actions on authentication related info of users.
 */
abstract class UserService extends IdentityService[IdentityImpl] {
  implicit val executionContext: ExecutionContext

  /**
   * Insert an identity.
   *
   * @param identity the identity to implement.
   * @return the user login info associated with the inserted identity.
   */
  def insert(identity: IdentityImpl): Future[UserXLoginInfo]

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create
    * a new user with the given profile.
    *
    * @param profile the social profile to save.
    * @param suffix the username suffix
    * @return the user for whom the profile was saved.
    */
  def save(
    profile: CommonSocialProfile, suffix: Option[Int]
  ): Future[IdentityImpl]
}

abstract class UserServiceDAO {
  val userXLoginInfoDAO: UserXLoginInfoDAO
  val userGroupDAO: UserGroupDAO
}

class UserServiceDAOImpl(
  protected val dbConfig: DatabaseConfig[JdbcProfile]
)(implicit val executionContext: ExecutionContext) extends UserServiceDAO
  with UserXLoginInfoDAOInterface
  with UserGroupDAOInterface {}

object UserService {
  def FormatToUsername(name: String): String = name.replaceAll("\\s+", "")
}

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl(
  protected val userServiceDAO: UserServiceDAO
)(implicit val executionContext: ExecutionContext) extends UserService {
  import UserService.FormatToUsername

  /**
   * Retrieves a user identity that matches the specified login info.
   *
   * @param loginInfo the login info to retrieve an identity.
   * @return the retrieved identity or None if no identity could be retrieved
   * for the given login info.
   */
  def retrieve(loginInfo: LoginInfo): Future[Option[IdentityImpl]] =
    userServiceDAO.userXLoginInfoDAO.getUserReadableFromLoginInfo(
      loginInfo
    ) map { _ match {
      case Some(user) => Some(
	IdentityImpl(user = user, loginInfo = loginInfo)
      )
      case None => None
    } }

  def insert(identity: IdentityImpl): Future[UserXLoginInfo] = {
    userServiceDAO.userXLoginInfoDAO.insertIdentity(identity).map(_._1)
  }

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create
    * a new user with the given profile.
    *
    * @param profile the social profile to save.
    * @param suffix the username suffix
    * @return the user for whom the profile was saved.
    */
  override def save(
    profile: CommonSocialProfile, suffix: Option[Int]
  ): Future[IdentityImpl] = retrieve(profile.loginInfo).flatMap {
    case Some(identityImpl) => Future.successful(identityImpl)
    case None => {
      val userId: String = UUID.randomUUID.toString
      val user = UserReadable(
	id = None,
	groupName = userId,
	roleName = "user",
	statusValue = "emailConfirmed",
	userId = userId,
	avatarURL = profile.avatarURL,
	email = profile.email.getOrElse(""),
	firstName = profile.firstName,
	lastName = profile.lastName,
	username = FormatToUsername(name = profile.fullName.getOrElse("")),
	usernameSuffix = suffix
      )
      val identity = IdentityImpl(user, profile.loginInfo)
      userServiceDAO.userXLoginInfoDAO.insertIdentity(identity).map {
	case (li, u) => identity.copy(user = user.copy(
	  id = u.id,
	  userId = u.userId,
	  avatarURL = u.avatarURL,
	  email = u.email,
	  firstName = u.firstName,
	  lastName = u.lastName,
	  username = u.username,
	  usernameSuffix = u.usernameSuffix
	))
      }
    }
  }
}
