package aurita.daos.auth

import com.mohiva.play.silhouette
import scala.concurrent.Future
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.{ IdentityImpl, User, UserXLoginInfo, UserReadable }

/** Provide the declaration of data access methods associated with the
  * [[UserXLoginInfoDAOInterface.UserXLoginInfoDAOImpl.tableQuery userXLoginInfo]] database table.
  *
  * The methods are defined in
  * [[UserXLoginInfoDAOInterface.UserXLoginInfoDAOImpl UserXLoginInfoDAOImpl]].
  *
  */
trait UserXLoginInfoDAO extends CRUD[UserXLoginInfo] {
  /**
   * Finds a user in [[aurita.model.auth.UserReadable readable]] format given its
   * [[aurita.model.auth.LoginInfo login info]].
   *
   * @param loginInfo The login info of the user to find.
   * @return The found [[aurita.model.auth.UserReadable user]] or None if no user for
   * the given login info could be found.
   */
  def getUserReadableFromLoginInfo(
    loginInfo: silhouette.api.LoginInfo
  ): Future[Option[UserReadable]]

  /**
   * Finds a [[aurita.model.auth.User user]] given its
   * [[aurita.model.auth.LoginInfo login info]].
   *
   * @param loginInfo The login info of the user to find.
   * @return The found [[aurita.model.auth.User user]] or None if no user for
   * the given login info could be found.
   */
  def findByLoginInfo(
    loginInfo: silhouette.api.LoginInfo
  ): Future[Option[User]]

  /**
   * Insert a [[aurita.model.auth.User user]], including creating the
   * [[aurita.models.auth.UserXGroup user join group.]]
   *
   * @param identity the [[aurita.models.auth.IdentityImpl identity]] of the user.
   * @return The tuple of the [[aurita.model.auth.UserXLoginInfo user join login info]]
   * and the [[aurita.models.auth.User user]].
   *
   */
  def insertIdentity(identity: IdentityImpl): Future[(UserXLoginInfo, User)]
}

/** Provides DAO interface for data access to the
  * [[UserXLoginInfoDAOInterface.UserXLoginInfoDAOImpl.tableQuery user login info]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[UserXLoginInfoDAO]] is accessed by mixing in
  * [[UserXLoginInfoDAOInterface]]
  * {{{
  * class Foo(id: Long) extends UserXLoginInfoDAOInterface {
  *   def fooFn: UserXLoginInfo = userXLoginInfoDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[UserXLoginInfoTable]]
  * {{{
  * class Foo() extends UserXLoginInfoDAOInterface {
  *   val tableQuery: TableQuery[UserXLoginInfoTable] = TableQuery[UserXLoginInfoTable]
  * }
  * }}}
  */
trait UserXLoginInfoDAOInterface extends DAOHelpers
  with LoginInfoDAOInterface
  with UserDAOInterface
  with UserXGroupDAOInterface {
  import profile.api._
  import com.softwaremill.macwire._
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The user login info data access implementation instance. */
  lazy val userXLoginInfoDAO: UserXLoginInfoDAO = wire[UserXLoginInfoDAOImpl]

  /** Implementation of data access methods associated with the
    * [[UserXLoginInfoDAOImpl.tableQuery user login info]] database table.
    */
  class UserXLoginInfoDAOImpl() extends UserXLoginInfoDAO with CRUDImpl[
    UserXLoginInfoTable, UserXLoginInfo
  ] {
    import java.util.UUID
    import aurita.models.auth.{ LoginInfo, UserGroup, UserXGroup }

    /** The user login info database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[UserXLoginInfoTable]
    protected lazy val loginInfoTableQuery = TableQuery[LoginInfoTable]
    protected lazy val userTableQuery = TableQuery[UserTable]
    protected lazy val groupTableQuery = TableQuery[UserGroupTable]
    protected lazy val userXGroupTableQuery = TableQuery[UserXGroupTable]
    protected lazy val roleTableQuery = TableQuery[UserRoleTable]
    protected lazy val statusTableQuery = TableQuery[CurrentStatusTable]

    def getUserReadableFromLoginInfo(
      loginInfo: silhouette.api.LoginInfo
    ): Future[Option[UserReadable]] =
      db.run(
        (for {
          (
            (
              (
                (
                  (uli, u),
                  li
                ),
                ug
              ),
              cs
            ),
            ur
          ) <- tableQuery join userTableQuery on (
            _.userId === _.id
          ) join loginInfoTableQuery on (
            _._1.loginInfoId === _.id
          ) join groupTableQuery on (
            _._1._2.groupId === _.id
          ) join statusTableQuery on (
            _._1._1._2.statusId === _.id
          ) join roleTableQuery on (
            _._1._1._1._2.roleId === _.id
          ) if li.providerId === loginInfo.providerID &&
            li.providerKey === loginInfo.providerKey
        } yield (
          ur.name,
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

    def findByLoginInfo(
      loginInfo: silhouette.api.LoginInfo
    ): Future[Option[User]] =
      db.run(
        (for {
          (
            (uli, u),
            li
          ) <- tableQuery join userTableQuery on (
            _.userId === _.id
          ) join loginInfoTableQuery on (
            _._1.loginInfoId === _.id
          ) if li.providerId === loginInfo.providerID &&
            li.providerKey === loginInfo.providerKey
        } yield (u)).result.headOption
      )

    def insertIdentity(identity: IdentityImpl): Future[(UserXLoginInfo, User)] =
      db.run(
        for {
          ugo <- groupTableQuery.filter(
            _.name === identity.user.groupName
          ).result.headOption
          _ <- loginInfoTableQuery.filter( li0 =>
            li0.providerId === identity.loginInfo.providerID &&
            li0.providerKey === identity.loginInfo.providerKey
          ).result.headOption flatMap {
            case Some(li1) => slick.dbio.DBIOAction.failed(
              new Exception(s"Duplicate login info exists: $li1")
            )
            case None => slick.dbio.DBIOAction.successful(true)
          }
          uo <- userTableQuery.filter(_.email === identity.user.email).result.headOption
          ug <- ugo match {
            case Some(g) => slick.dbio.DBIOAction.successful(g)
            case None =>
              (groupTableQuery returning groupTableQuery.map(_.id) into (
                (ans, id) => ans.copy(id = Option(id))
              )) += UserGroup(
                id = None,
                active = true,
                description = "A user group",
                name = identity.user.groupName
              )
          }
          cs0 <- statusDAO.getStatus(
            key = "user", value = identity.user.statusValue
          )
          cs <- cs0 match {
            case Seq() => slick.dbio.DBIOAction.failed(
              new Exception(s"No user status found for value: ${identity.user.statusValue}")
            )
            case head0 +: tail0 => slick.dbio.DBIOAction.successful(head0)
          }
          ur0 <- userRoleDAO.getRole(
            name = identity.user.roleName
          )
          ur <- ur0 match {
            case Seq() => slick.dbio.DBIOAction.failed(
              new Exception(s"No role found for name: ${identity.user.roleName}")
            )
            case head0 +: tail0 => slick.dbio.DBIOAction.successful(head0)
          }
          u <- uo match {
            case Some(user) => {
              val updatedUser = user.copy(
                statusId = cs.id.get,
                firstName = user.firstName.orElse(identity.user.firstName),
                lastName = user.lastName.orElse(identity.user.lastName),
                avatarURL = user.avatarURL.orElse(identity.user.avatarURL),
                username = user.username
              )
              userTableQuery.insertOrUpdate(updatedUser).map(_ => updatedUser)
            }
            case None =>
              (userTableQuery returning userTableQuery.map(_.id) into (
                (ans, id) => ans.copy(id = Option(id))
              )) += User(
                id = None,
                groupId = ug.id.get,
                roleId = ur.id.get,
                statusId = cs.id.get,
                userId = identity.user.userId,
                firstName = identity.user.firstName,
                lastName = identity.user.lastName,
                email = identity.user.email,
                avatarURL = identity.user.avatarURL,
                username = identity.user.username,
                usernameSuffix = identity.user.usernameSuffix
              )
          }
          uxgo <- userXGroupTableQuery.filter(uxg =>
            uxg.groupId === ug.id.get &&
            uxg.userId === u.id.get &&
            uxg.userPermissionId === 1L
          ).sortBy(_.id.desc).result.headOption
          uxg <- uxgo match {
            case Some(uxg1) => slick.dbio.DBIOAction.successful(uxg1)
            case None =>
              (userXGroupTableQuery returning userXGroupTableQuery.map(_.id) into (
                (ans, id) => ans.copy(id = Option(id)))
              ) += UserXGroup(
                id = None,
                groupId = ug.id.get,
                userId = u.id.get,
                userPermissionId = 1
              )
          }
          li <- (
            loginInfoTableQuery returning loginInfoTableQuery.map(_.id)
              into ((ans, id) => ans.copy(id = Option(id)))
          ) += LoginInfo(
            id = None,
            providerId = identity.loginInfo.providerID,
            providerKey = identity.loginInfo.providerKey
          )
          uli <- (
            tableQuery returning tableQuery.map(_.id)
              into ((ans, id) => ans.copy(id = Option(id)))
          ) += UserXLoginInfo(
            id = None,
            loginInfoId = li.id.get,
            userId = u.id.get
          )
        } yield (uli, u)
      )
  }

  /** The user login info database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class UserXLoginInfoTable(tag: Tag) extends RichTable[UserXLoginInfo](
    tag = tag, name = "USER_X_LOGIN_INFO"
  ) {
    def loginInfoId = column[Long]("LOGIN_INFO_ID")

    def userId = column[Long]("USER_ID")

    def loginInfoFK =
      foreignKey(
        "UXLINFO_LOGIN_INFO_FK", loginInfoId, TableQuery[LoginInfoTable]
      )(_.id)

    def userFK =
      foreignKey("UXLINFO_USER_FK", userId, TableQuery[UserTable])(_.id)

    def loginInfoIdUC =
      index("UXLINFO_LOGIN_INFO_ID_UC", loginInfoId, unique = true)

    def * = (
      id.?,
      loginInfoId,
      userId
    ) <> ((UserXLoginInfo.apply _).tupled, UserXLoginInfo.unapply)

    def ? = (
      id.?,
      loginInfoId,
      userId
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (UserXLoginInfo.apply _).tupled(
            (_1, _2, _3)
          ))
      },
      maybeUnapply
    )
  }
}
