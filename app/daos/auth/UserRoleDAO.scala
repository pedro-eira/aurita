package aurita.daos.auth

import scala.concurrent.Future
import slick.dbio.Effect.Read
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.UserRole

/** Provide the declaration of data access methods associated with the
  * [[UserRoleDAOInterface.UserRoleDAOImpl.tableQuery user role]]
  * database table.
  *
  * The methods are defined in
  * [[UserRoleDAOInterface.UserRoleDAOImpl UserRoleDAOImpl]].
  *
  */
trait UserRoleDAO extends CRUD[UserRole] {
  import slick.dbio.{ DBIOAction, NoStream }

  /** Get the user role row associate with a given role name.
    *
    * @param name the [[aurita.models.auth.UserRole role]] name.
    * @return the user role row.
    */
  def getRole(name: String): DBIOAction[Seq[UserRole], NoStream, Read]
}

/** Provides DAO interface for data access to the
  * [[UserRoleDAOInterface.UserRoleDAOImpl.tableQuery user role]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[UserRoleDAO]] is accessed by mixing in
  * [[UserRoleDAOInterface]]
  * {{{
  * class Foo(id: Long) extends UserRoleDAOInterface {
  *   def fooFn: UserRole = userRoleDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[UserRoleTable]]
  * {{{
  * class Foo() extends UserRoleDAOInterface {
  *   val tableQuery: TableQuery[UserRoleTable] =
  *     TableQuery[UserRoleTable]
  * }
  * }}}
  */
trait UserRoleDAOInterface extends DAOHelpers {
  import profile.api._
  import com.softwaremill.macwire.wire
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The user role data access implementation instance. */
  lazy val userRoleDAO: UserRoleDAO = wire[UserRoleDAOImpl]

  /** Implementation of data access methods associated with the
    * [[UserRoleDAOImpl.tableQuery user role database table]].
    */
  class UserRoleDAOImpl() extends UserRoleDAO
    with CRUDImpl[UserRoleTable, UserRole] {

    /** The user role database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[UserRoleTable]

    def getRole(
      name: String
    ): DBIOAction[Seq[UserRole], NoStream, Read] = {
      tableQuery.filter(s => s.name === name).result
    }
  }

  /** The user role database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class UserRoleTable(tag: Tag)
    extends RichTable[UserRole](
      tag = tag, name = "USER_ROLE", idTypeInfo = "INT UNSIGNED NOT NULL"
    ) {
    def active = column[Boolean]("ACTIVE", DBType("BOOLEAN NOT NULL"))

    def description = column[String](
      "DESCRIPTION", O.Length(length = 254, varying = true)
    )

    def name =
      column[String]("ROLE_NAME", O.Length(length = 32, varying = true))

    def selectable = column[Boolean]("SELECTABLE", DBType("BOOLEAN NOT NULL"))

    def nameUC = index("UROLE_NAME_UC", name, unique = true)

    def * = (
      id.?,
      active,
      description,
      name,
      selectable
    ) <> ((UserRole.apply _).tupled, UserRole.unapply)

    def ? = (
      id.?,
      active,
      description,
      name,
      selectable
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (UserRole.apply _).tupled((_1, _2, _3, _4, _5)))
      },
      maybeUnapply
    )
  }
}
