package aurita.daos.auth

import scala.concurrent.Future
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.UserGroup

/** Provide the declaration of data access methods associated with the
  * [[UserGroupDAOInterface.UserGroupDAOImpl.tableQuery user group]]
  * database table.
  *
  * The methods are defined in
  * [[UserGroupDAOInterface.UserGroupDAOImpl UserGroupDAOImpl]].
  *
  */
trait UserGroupDAO extends CRUD[UserGroup] {}

/** Provides DAO interface for data access to the
  * [[UserGroupDAOInterface.UserGroupDAOImpl.tableQuery user group]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[UserGroupDAO]] is accessed by mixing in
  * [[UserGroupDAOInterface]]
  * {{{
  * class Foo(id: Long) extends UserGroupDAOInterface {
  *   def fooFn: UserGroup = userGroupDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[UserGroupTable]]
  * {{{
  * class Foo() extends UserGroupDAOInterface {
  *   val tableQuery: TableQuery[UserGroupTable] =
  *     TableQuery[UserGroupTable]
  * }
  * }}}
  */
trait UserGroupDAOInterface extends DAOHelpers {
  import profile.api._
  import com.softwaremill.macwire.wire
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The user group data access implementation instance. */
  lazy val userGroupDAO: UserGroupDAO = wire[UserGroupDAOImpl]

  /** Implementation of data access methods associated with the
    * [[UserGroupDAOImpl.tableQuery user group database table]].
    */
  class UserGroupDAOImpl() extends UserGroupDAO
    with CRUDImpl[UserGroupTable, UserGroup] {
    import scala.util.Try

    /** The user group database table.
      * @see [[http://slick.lightbend.com/doc/3.2.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[UserGroupTable]
  }

  /** The user group database table row.
    * @see [[http://slick.lightbend.com/doc/3.2.0/schemas.html#table-rows]]
    */
  class UserGroupTable(tag: Tag) extends RichTable[UserGroup](
    tag = tag, name = "USER_GROUP"
  ) {
    def active = column[Boolean]("ACTIVE")

    def description = column[String](
      "DESCRIPTION", O.Length(length = 254, varying = true)
    )

    def name =
      column[String]("GROUP_NAME", O.Length(length = 127, varying = true))

    def nameUC = index("UGROUP_NAME_UC", name, unique = true)

    def * = (
      id.?,
      active,
      description,
      name
    ) <> ((UserGroup.apply _).tupled, UserGroup.unapply)

    def ? = (
      id.?,
      active,
      description,
      name
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (UserGroup.apply _).tupled((_1, _2, _3, _4)))
      },
      maybeUnapply
    )
  }
}
