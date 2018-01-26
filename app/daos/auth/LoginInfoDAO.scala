package aurita.daos.auth

import com.mohiva.play.silhouette
import scala.concurrent.Future
import slick.dbio.Effect.Read
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.LoginInfo

/** Provide the declaration of data access methods associated with the
  * [[LoginInfoDAOInterface.LoginInfoDAOImpl.tableQuery loginInfo]] database table.
  *
  * The methods are defined in
  * [[LoginInfoDAOInterface.LoginInfoDAOImpl LoginInfoDAOImpl]].
  *
  */
trait LoginInfoDAO extends CRUD[LoginInfo] {
  import slick.dbio.{ DBIOAction, NoStream }

  /**
   * Get all [[aurita.model.auth.LoginInfo login profiles]] associated with a provider's
   * [[silhouette.api.LoginInfo login profile]].
   *
   * @param loginInfo The provider's [[silhouette.api.LoginInfo login profile.]]
   * @return all the [[aurita.model.auth.LoginInfo login profiles.]]
   *
   */
  def getLoginInfos(
    loginInfo: silhouette.api.LoginInfo
  ): slick.lifted.Query[LoginInfoDAOInterface#LoginInfoTable, LoginInfo, Seq]

  /**
   * Get latest [[aurita.model.auth.LoginInfo login profile]] associated with a provider's
   * [[silhouette.api.LoginInfo login profile]].
   *
   * @param loginInfo The provider's [[silhouette.api.LoginInfo login profile.]]
   * @return The found [[aurita.model.auth.LoginInfo login profile.]]
   *
   */
  def getLoginInfo(
    loginInfo: silhouette.api.LoginInfo
  ): DBIOAction[LoginInfo, NoStream, Read]
}

/** Provides DAO interface for data access to the
  * [[LoginInfoDAOInterface.LoginInfoDAOImpl.tableQuery login info]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[LoginInfoDAO]] is accessed by mixing in
  * [[LoginInfoDAOInterface]]
  * {{{
  * class Foo(id: Long) extends LoginInfoDAOInterface {
  *   def fooFn: LoginInfo = loginInfoDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[LoginInfoTable]]
  * {{{
  * class Foo() extends LoginInfoDAOInterface {
  *   val tableQuery: TableQuery[LoginInfoTable] = TableQuery[LoginInfoTable]
  * }
  * }}}
  */
trait LoginInfoDAOInterface extends DAOHelpers {
  import profile.api._
  import com.softwaremill.macwire._
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The login info data access implementation instance. */
  lazy val loginInfoDAO: LoginInfoDAO = wire[LoginInfoDAOImpl]

  /** Implementation of data access methods associated with the
    * [[LoginInfoDAOImpl.tableQuery login info]] database table.
    */
  class LoginInfoDAOImpl() extends LoginInfoDAO with CRUDImpl[
    LoginInfoTable, LoginInfo
  ] {

    /** The login info database table.
     * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
     */
    protected val tableQuery = TableQuery[LoginInfoTable]

    def getLoginInfos(
      loginInfo: silhouette.api.LoginInfo
    ): slick.lifted.Query[LoginInfoTable, LoginInfo, Seq] = tableQuery.filter(
      li => li.providerId === loginInfo.providerID &&
        li.providerKey === loginInfo.providerKey
    )

    def getLoginInfo(
      loginInfo: silhouette.api.LoginInfo
    ): DBIOAction[LoginInfo, NoStream, Read] = getLoginInfos(
      loginInfo = loginInfo
    ).result.headOption flatMap { _ match {
      case Some(li0) => slick.dbio.DBIOAction.successful(li0)
      case None => slick.dbio.DBIOAction.failed(
        new Exception(s"Login Info not found for provider's ${loginInfo}")
      )
    } }
  }

  /** The login info database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class LoginInfoTable(tag: Tag) extends RichTable[LoginInfo](
    tag = tag, name = "LOGIN_INFO"
  ) {
    def providerId =
      column[String]("PROVIDER_ID", O.Length(length = 254, varying = true))

    def providerKey =
      column[String]("PROVIDER_KEY", O.Length(length = 254, varying = true))

    def providerIdProviderKeyUC = index(
      "LINFO_PROVIDER_ID_PROVIDER_KEY_UC",
      (providerId, providerKey),
      unique = true
    )

    def * = (
      id.?,
      providerId,
      providerKey
    ) <> ((LoginInfo.apply _).tupled, LoginInfo.unapply)

    def ? = (
      id.?,
      providerId,
      providerKey
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (LoginInfo.apply _).tupled(
            (_1, _2, _3)
          ))
      },
      maybeUnapply
    )
  }
}
