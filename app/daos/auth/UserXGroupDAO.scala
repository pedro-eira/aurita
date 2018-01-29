package aurita.daos.auth

import scala.concurrent.Future
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.UserXGroup

/** Provide the declaration of data access methods associated with the
  * [[UserXGroupDAOInterface.UserXGroupDAOImpl.tableQuery user group]]
  * database table.
  *
  * The methods are defined in
  * [[UserXGroupDAOInterface.UserXGroupDAOImpl UserXGroupDAOImpl]].
  *
  */
trait UserXGroupDAO extends CRUD[UserXGroup] {}

/** Provides DAO interface for data access to the
  * [[UserXGroupDAOInterface.UserXGroupDAOImpl.tableQuery user group join]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[UserXGroupDAO]] is accessed by mixing in
  * [[UserXGroupDAOInterface]]
  * {{{
  * class Foo(id: Long) extends UserXGroupDAOInterface {
  *   def fooFn: UserXGroup = userXGroupDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[UserXGroupTable]]
  * {{{
  * class Foo() extends UserXGroupDAOInterface {
  *   val tableQuery: TableQuery[UserXGroupTable] =
  *     TableQuery[UserXGroupTable]
  * }
  * }}}
  */
trait UserXGroupDAOInterface extends DAOHelpers
  with UserGroupDAOInterface
  with UserDAOInterface
  with UserPermissionDAOInterface {
  import profile.api._
  import com.softwaremill.macwire.wire
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The user group data access implementation instance. */
  lazy val userXGroupDAO: UserXGroupDAO = wire[UserXGroupDAOImpl]

  /** Implementation of data access methods associated with the
    * [[UserXGroupDAOImpl.tableQuery user group database table]].
    */
  class UserXGroupDAOImpl() extends UserXGroupDAO
    with CRUDImpl[UserXGroupTable, UserXGroup] {

    /** The user group database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[UserXGroupTable]
  }

  /** The user group database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class UserXGroupTable(tag: Tag) extends RichTable[UserXGroup](
    tag = tag, name = "USER_X_GROUP"
  ) {
    def groupId = column[Long]("GROUP_ID", DBType("BIGINT NOT NULL"))

    def userId = column[Long]("USER_ID", DBType("BIGINT NOT NULL"))

    def userPermissionId = column[Long](
      "USER_PERMISSION_ID", DBType("INT UNSIGNED NOT NULL")
    )

    def userXGroupUC =
      index("UXGROUP_USER_GROUP_UC", (userId, groupId), unique = true)

    def groupFK = foreignKey(
      "UXGROUP_GROUP_FK", groupId, TableQuery[UserGroupTable]
    )(_.id)

    def userFK =
      foreignKey("UXGROUP_USER_FK", userId, TableQuery[UserTable])(_.id)

    def userPermissionFK = foreignKey(
      "UXGROUP_USER_PERMISSION_FK", userPermissionId, TableQuery[UserPermissionTable]
    )(_.id)

    def * = (
      id.?,
      groupId,
      userId,
      userPermissionId
    ) <> ((UserXGroup.apply _).tupled, UserXGroup.unapply)

    def ? = (
      id.?,
      groupId,
      userId,
      userPermissionId
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (UserXGroup.apply _).tupled((_1, _2, _3, _4)))
      },
      maybeUnapply
    )
  }
}
