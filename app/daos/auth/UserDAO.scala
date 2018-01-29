package aurita.daos.auth

import java.sql.Timestamp
import scala.concurrent.Future
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.{ User, UserReadable }
import aurita.daos.CurrentStatusDAOInterface

/** Provide the declaration of data access methods associated with the
  * [[UserDAOInterface.UserDAOImpl.tableQuery user]] database table.
  *
  * The methods are defined in
  * [[UserDAOInterface.UserDAOImpl UserDAOImpl]].
  *
  */
trait UserDAO extends CRUD[User] {
  /**
    * Checks whether there is an [[aurita.models.auth.User user]].
    *
    * @param username the username of the user.
    * @param suffix the suffix attached to the username.
    * @return whether there is an existing user with the given username and suffix.
    */
  def fullUsernameExists(
    username: String, suffix: Option[Int]
  ): Future[Boolean]

  /**
    * Find a specific [[aurita.models.auth.UserReadable user readable.]]
    *
    * @param id the id of the user.
    * @return the user in readable format.
    *
    */
  def findUserReadableById(id: Long): Future[Option[UserReadable]]

   /** Finds a specific [[aurita.models.auth.User user]] row.
    *
    * @param email the email of the user.
    * @return the user with the email.
    *
    */
  def findByEmail(email: String): Future[Option[User]]

   /** Find a specific [[aurita.models.auth.User user]] row.
    *
    * @param username the full username of the user i.e. including the suffix.
    * @return the user with the username.
    *
    */
  def findByUsername(username: String): Future[Option[User]]

   /** Checks if a username or email of a [[aurita.models.auth.User user]] are valid.
    *
    * @param username the full username of the user i.e. including the suffix.
    * @param email the email of the user.
    * @return true if either the username or email is valid.
    *
    */
  def isValidUsernameOrEmail(username: String, email: String): Future[Boolean]

   /** Updates the [[aurita.models.auth.CurrentStatus status]] of a [[aurita.models.auth.User user]].
    *
    * @param id the user primary key.
    * @param statusValue the new status value.
    * @return the number of rows updated.
    *
    */
  def updateStatus(id: Long, statusValue: String): Future[Int]

  /** Retrieves the number of rows in the [[aurita.models.auth.User user]] table.
    *
    * @return the number of user rows.
    *
    */
  def length: Future[Int]

  /** Retrieves all users in the [[aurita.models.auth.User user]] table.
    *
    * @return the sequence of all users.
    */
  def all: Future[Seq[User]]
}

object UserDAO {

  case class DuplicateUsernameException(
    message: String, cause: Throwable = None.orNull
  ) extends Exception(message, cause)

  case class DuplicateEmailException(
    message: String, cause: Throwable = None.orNull
  ) extends Exception(message, cause)

  case class BadDomainNameException(
    message: String, cause: Throwable = None.orNull
  ) extends Exception(message, cause)

  case class UnknownDomainNameException(
    message: String, cause: Throwable = None.orNull
  ) extends Exception(message, cause)

}

/** Provides DAO interface for data access to the
  * [[UserDAOInterface.UserDAOImpl.tableQuery user]] database table.
  *
  * ==Overview==
  * The implemented instance of [[UserDAO]] is accessed by mixing in
  * [[UserDAOInterface]]
  * {{{
  * class Foo(id: Long) extends UserDAOInterface {
  *   def fooFn: User = userDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[UserTable]]
  * {{{
  * class Foo() extends UserDAOInterface {
  *   val tableQuery: TableQuery[UserTable] = TableQuery[UserTable]
  * }
  * }}}
  */
trait UserDAOInterface extends DAOHelpers
  with UserGroupDAOInterface
  with UserRoleDAOInterface
  with CurrentStatusDAOInterface {
  import profile.api._
  import com.softwaremill.macwire._
  import slick.dbio.Effect.Read
  import UserDAO._
  import java.net.InetAddress
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The user data access implementation instance. */
  lazy val userDAO: UserDAO = wire[UserDAOImpl]

  /** Implementation of data access methods associated with the
    * [[UserDAOImpl.tableQuery user]] database table.
    */
  class UserDAOImpl() extends UserDAO with CRUDImpl[UserTable, User] {

    /** The user database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[UserTable]
    protected lazy val groupTableQuery = TableQuery[UserGroupTable]
    protected lazy val roleTableQuery = TableQuery[UserRoleTable]
    protected lazy val statusTableQuery = TableQuery[CurrentStatusTable]

    lazy val logger = new aurita.utility.messages.Log(this.getClass)

    def length: Future[Int] = db.run(tableQuery.length.result)

    /**
      * Retrieves all users from db
      *
      * @return seq of all users stored in db
      */
    def all: Future[Seq[User]] = db.run(tableQuery.result)

    def findUserReadableById(id: Long): Future[Option[UserReadable]] =
      db.run(
        (for {
          (
            (
              (
                (u, ug),
                cs
              ),
              ur0
            )
          ) <- tableQuery join groupTableQuery on (
            _.groupId === _.id
          ) join statusTableQuery on (
            _._1.statusId === _.id
          ) join roleTableQuery on (
            _._1._1.roleId === _.id
          ) if u.id === id
        } yield (
          ur0.name,
          cs.value,
          u.userId,
          ug.name,
          u.avatarURL,
          u.email,
          u.firstName,
          u.lastName,
          u.id,
          u.username,
          u.usernameSuffix
        )).result.headOption.map {
          case Some(ur) => Some(UserReadable(
            id = Option(ur._9),
            roleName = ur._1,
            statusValue = ur._2,
            userId = ur._3,
            groupName = ur._4,
            avatarURL = ur._5,
            email = ur._6,
            firstName = ur._7,
            lastName = ur._8,
            username = ur._10,
            usernameSuffix = ur._11
          ))
          case None => None
        }
      )

    def findByEmail(email: String): Future[Option[User]] = db.run(
      _findByEmail(email = email)
    )

    def findByUsername(username: String): Future[Option[User]] =
      db.run(_findByUsername(username = username))

    def isValidUsernameOrEmail(
      username: String, email: String
    ): Future[Boolean] = db.run(
      _isValidUsernameOrEmail(username = username, email = email)
    )

    def updateStatus(id: Long, statusValue: String): Future[Int] = {
      val a =
        {
          val q = tableQuery.filter(_.id === id)
          for {
            csOption <- statusDAO.getStatus(
              key = "user", value = statusValue
            ).map(_.headOption)
            rowsAffected <- q.map(u =>
              u.statusId
            ).update(csOption.get.id.get)
          } yield rowsAffected
        }
      db.run(a)
    }

    /**
      * Checks whether there is existing user by username and suffix
      *
      * @param username the username
      * @param suffix the username suffix
      * @return whether there is existing user by username and postfix
      */
    def fullUsernameExists(
      username: String, suffix: Option[Int]
    ): Future[Boolean] = db.run(
      tableQuery.filter(
        u => u.usernameSuffix === suffix && u.username === username
      ).exists.result
    )

    private def _findByEmail(
      email: String
    ): DBIOAction[Option[User], NoStream, Read] = tableQuery.filter(
      _.email === email
    ).result.headOption

    private def _findByUsername(
      username: String
    ): DBIOAction[Option[User], NoStream, Read] = tableQuery.filter(
      _.username === username
    ).result.headOption

    private def _isValidUsernameOrEmail(
      username: String, email: String
    ): DBIOAction[Boolean, NoStream, Read] = (for {
      u0 <- _findByUsername(username = username) flatMap ( r => r match {
        case Some(_) => slick.dbio.DBIOAction.failed(
          DuplicateUsernameException(s"${username} already used")
        )
        case None => _findByEmail(email = email) flatMap ( r0 => r0 match {
          case Some(_) => slick.dbio.DBIOAction.failed(
            DuplicateEmailException(s"${email} already used")
          )
          case None => slick.dbio.DBIOAction.successful(true)
        } )
      } )
      h0 <- try {
        val h1 = InetAddress.getAllByName(email.split("@").tail.head)
        logger.info(s"${email} hostname checkup: ${h1}")
        slick.dbio.DBIOAction.successful(true)
      } catch {
        case e: java.net.UnknownHostException => slick.dbio.DBIOAction.failed(
          BadDomainNameException(s"Email domain name does not exist: ${e}")
        )
        case e: Exception => slick.dbio.DBIOAction.failed(
          UnknownDomainNameException(s"Email domain name lookup error: ${e}")
        )
      }
    } yield (u0, h0) match {
      case (true, true) => true
      case (_, _) => false
    })
  }

  /** The user database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class UserTable(tag: Tag) extends RichTable[User](
    tag = tag, name = "USER"
  ) {
    def groupId = column[Long]("GROUP_ID", DBType("BIGINT NOT NULL"))

    def roleId = column[Long]("ROLE_ID", DBType("INT UNSIGNED NOT NULL"))

    def statusId = column[Long]("STATUS_ID", DBType("INT UNSIGNED NOT NULL"))

    def userId =
      column[String]("USER_ID", O.Length(length = 36, varying = false))

    def avatarURL = column[Option[String]](
      "AVATAR_URL", O.Length(length = 254, varying = true)
    )

    def email =
      column[String]("EMAIL", O.Length(length = 127, varying = true))

    def firstName = column[Option[String]](
      "FIRST_NAME", O.Length(length = 64, varying = true)
    )

    def lastName =
      column[Option[String]]("LAST_NAME", O.Length(length = 64, varying = true))

    def fullName: Rep[String] = (
      firstName.getOrElse("") ++ " " ++ lastName.getOrElse("")
    ).trim

    def createdTime = column[Timestamp](
      "CREATED_TIME",
      DBType("TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    )

    def username =
      column[String]("USERNAME", O.Length(length = 127, varying = true))

    def usernameSuffix = column[Option[Int]](
      "USERNAME_SUFFIX", DBType("INT UNSIGNED")
    )

    def usernameSuffixString: Rep[String] =
      usernameSuffix.asColumnOf[Option[String]].getOrElse("")

    def fullUsername: Rep[String] = username ++ usernameSuffixString

    def userIdUC = index("USER_USER_ID_UC", userId, unique = true)

    def emailUC = index("USER_EMAIL_UC", email, unique = true)

    def usernameSuffixUC = index(
      "USER_USERNAME_SUFFIX_UC", (username, usernameSuffix), unique = true
    )

    def groupFK =
      foreignKey(
        "USER_GROUP_FK",
        groupId,
        TableQuery[UserGroupTable]
      )(_.id)

    def roleFK =
      foreignKey(
        "USER_ROLE_FK",
        roleId,
        TableQuery[UserRoleTable]
      )(_.id)

    def statusFK =
      foreignKey(
        "USER_STATUS_FK",
        statusId,
        TableQuery[CurrentStatusTable]
      )(_.id)

    def * = (
      id.?,
      groupId,
      roleId,
      statusId,
      userId,
      avatarURL,
      email,
      firstName,
      lastName,
      username,
      usernameSuffix
    ) <> ((User.apply _).tupled, User.unapply)

    def ? = (
      id.?,
      groupId,
      roleId,
      statusId,
      userId,
      avatarURL,
      email,
      firstName,
      lastName,
      username,
      usernameSuffix
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (User.apply _).tupled(
            (_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11))
          )
      },
      maybeUnapply
    )
  }
}
