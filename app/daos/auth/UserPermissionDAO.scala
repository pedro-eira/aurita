package aurita.daos.auth

import scala.concurrent.Future
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.UserPermission

/** Provide the declaration of data access methods associated with the
  * [[UserPermissionDAOInterface.UserPermissionDAOImpl.tableQuery user permission]]
  * database table.
  *
  * The methods are defined in
  * [[UserPermissionDAOInterface.UserPermissionDAOImpl UserPermissionDAOImpl]].
  *
  */
trait UserPermissionDAO extends CRUD[UserPermission] {}

/** Provides DAO interface for data access to the
  * [[UserPermissionDAOInterface.UserPermissionDAOImpl.tableQuery user permission]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[UserPermissionDAO]] is accessed by mixing in
  * [[UserPermissionDAOInterface]]
  * {{{
  * class Foo(id: Long) extends UserPermissionDAOInterface {
  *   def fooFn: UserPermission = userPermissionDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[UserPermissionTable]]
  * {{{
  * class Foo() extends UserPermissionDAOInterface {
  *   val tableQuery: TableQuery[UserPermissionTable] =
  *     TableQuery[UserPermissionTable]
  * }
  * }}}
  */
trait UserPermissionDAOInterface extends DAOHelpers
  with UserRoleDAOInterface {
  import profile.api._
  import com.softwaremill.macwire.wire
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The user permission data access implementation instance. */
  lazy val userPermissionDAO: UserPermissionDAO = wire[UserPermissionDAOImpl]

  /** Implementation of data access methods associated with the
    * [[UserPermissionDAOImpl.tableQuery user permission database table]].
    */
  class UserPermissionDAOImpl() extends UserPermissionDAO
    with CRUDImpl[UserPermissionTable, UserPermission] {

    /** The user permission database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[UserPermissionTable]
  }

  /** The user permission database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class UserPermissionTable(tag: Tag)
    extends RichTable[UserPermission](
      tag = tag, name = "USER_PERMISSION", idTypeInfo = "INT UNSIGNED NOT NULL"
    ) {
    def roleId = column[Long]("ROLE_ID", DBType("INT UNSIGNED NOT NULL"))

    def active = column[Boolean]("ACTIVE", DBType("BOOLEAN NOT NULL"))

    def read = column[Boolean]("READ_PERM", DBType("BOOLEAN NOT NULL"))

    def write = column[Boolean]("WRITE_PERM", DBType("BOOLEAN NOT NULL"))

    def execute = column[Boolean]("EXECUTE_PERM", DBType("BOOLEAN NOT NULL"))

    def roleIdReadWriteExecuteUC = index(
      "UPERM_ROLE_READ_WRITE_EXECUTE_UC",
      (roleId, read, write, execute),
      unique = true
    )

    def roleFK =
      foreignKey("UPERM_ROLE_FK", roleId, TableQuery[UserRoleTable])(_.id)

    def * = (
      id.?,
      roleId,
      active,
      read,
      write,
      execute
    ) <> ((UserPermission.apply _).tupled, UserPermission.unapply)

    def ? = (
      id.?,
      roleId,
      active,
      read,
      write,
      execute
    ).shaped <> (
      {
	r =>
	  import r._
	  _1.map(_ => (UserPermission.apply _).tupled((_1, _2, _3, _4, _5, _6)))
      },
      maybeUnapply
    )
  }
}
