package aurita.daos

import scala.concurrent.Future
import slick.dbio.Effect.Read
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.CurrentStatus

/** Provide the declaration of data access methods associated with the
  * [[CurrentStatusDAOInterface.CurrentStatusDAOImpl.tableQuery status]]
  * database table.
  *
  * The methods are defined in
  * [[CurrentStatusDAOInterface.CurrentStatusDAOImpl CurrentStatusDAOImpl]].
  *
  */
trait CurrentStatusDAO extends CRUD[CurrentStatus] {
  import slick.dbio.{ DBIOAction, NoStream }

  /** Get the current status row associate with a given status key and value.
    *
    * @param key the [[aurita.models.CurrentStatus status]] key.
    * @param key the [[aurita.models.CurrentStatus status]] value.
    * @return the current status row.
    */
  def getStatus(
    key: String, value: String
  ): DBIOAction[Seq[CurrentStatus], NoStream, Read]
}

/** Provides DAO interface for data access to the
  * [[CurrentStatusDAOInterface.CurrentStatusDAOImpl.tableQuery status]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[CurrentStatusDAO]] is accessed by mixing in
  * [[CurrentStatusDAOInterface]]
  * {{{
  * class Foo(id: Long) extends CurrentStatusDAOInterface {
  *   def fooFn: CurrentStatus = statusDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[CurrentStatusTable]]
  * {{{
  * class Foo() extends CurrentStatusDAOInterface {
  *   val tableQuery: TableQuery[CurrentStatusTable] =
  *     TableQuery[CurrentStatusTable]
  * }
  * }}}
  */
trait CurrentStatusDAOInterface extends DAOHelpers {
  import profile.api._
  import com.softwaremill.macwire.wire
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The status data access implementation instance. */
  lazy val statusDAO: CurrentStatusDAO = wire[CurrentStatusDAOImpl]

  /** Implementation of data access methods associated with the
    * [[CurrentStatusDAOImpl.tableQuery status]] database table.
    */
  class CurrentStatusDAOImpl() extends CurrentStatusDAO
    with CRUDImpl[CurrentStatusTable, CurrentStatus] {

    /** The status database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[CurrentStatusTable]

    def getStatus(
      key: String, value: String
    ): DBIOAction[Seq[CurrentStatus], NoStream, Read] = {
      tableQuery.filter(s => s.key === key && s.value === value).sortBy(_.id.desc).result
    }

  }

  /** The status database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class CurrentStatusTable(tag: Tag)
    extends RichTable[CurrentStatus](
      tag = tag, name = "CURRENT_STATUS", idTypeInfo = "int unsigned not null"
    ) {
    def active = column[Boolean]("ACTIVE", DBType("BOOLEAN NOT NULL"))

    def description = column[String](
      "DESCRIPTION", O.Length(length = 254, varying = true)
    )

    def key =
      column[String]("STATUS_KEY", O.Length(length = 32, varying = true))

    def value =
      column[String]("STATUS_VALUE", O.Length(length = 32, varying = true))

    def keyValueUC = index("CSTATUS_KEY_VALUE_UC", (key, value), unique = true)

    def * = (
      id.?,
      active,
      description,
      key,
      value
    ) <> ((CurrentStatus.apply _).tupled, CurrentStatus.unapply)

    def ? = (
      id.?,
      active,
      description,
      key,
      value
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (CurrentStatus.apply _).tupled((_1, _2, _3, _4, _5)))
      },
      maybeUnapply
    )
  }
}
