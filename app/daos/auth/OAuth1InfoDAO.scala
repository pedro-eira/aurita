package aurita.daos.auth

import com.mohiva.play.silhouette
import silhouette.persistence.daos.DelegableAuthInfoDAO
import scala.concurrent.Future

import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.OAuth1Info

/** Provide the declaration of data access methods associated with the
  * [[OAuth1InfoDAOInterface.OAuth1InfoDAOImpl.tableQuery oauth1Info]] database table.
  *
  * The methods are defined in
  * [[OAuth1InfoDAOInterface.OAuth1InfoDAOImpl OAuth1InfoDAOImpl]].
  *
  */
trait OAuth1InfoDAO extends DelegableAuthInfoDAO[silhouette.impl.providers.OAuth1Info]
  with CRUD[OAuth1Info] {

  /**
    * Finds the auth info which is linked with the specified login info.
    *
    * @param loginInfo The linked login info.
    * @return the retrieved auth info or None if no auth info could be retrieved for the
    * given login info.
    */
  def find(
    loginInfo: silhouette.api.LoginInfo
  ): Future[Option[silhouette.impl.providers.OAuth1Info]]

  /**
    * Adds new auth info for the given login info.
    *
    * @param loginInfo The login info for which the auth info should be added.
    * @param authInfo The auth info to add.
    * @return The added auth info.
    */
  def add(
    loginInfo: silhouette.api.LoginInfo, authInfo: silhouette.impl.providers.OAuth1Info
  ): Future[silhouette.impl.providers.OAuth1Info]

  /**
    * Updates the auth info for the given login info.
    *
    * @param loginInfo The login info for which the auth info should be updated.
    * @param authInfo The auth info to update.
    * @return The updated auth info.
    */
  def update(
    loginInfo: silhouette.api.LoginInfo, authInfo: silhouette.impl.providers.OAuth1Info
  ): Future[silhouette.impl.providers.OAuth1Info]

  /**
    * Saves the auth info for the given login info.
    *
    * This method either adds the auth info if it doesn't exists or it updates the auth info
    * if it already exists.
    *
    * @param loginInfo The login info for which the auth info should be saved.
    * @param authInfo The auth info to save.
    * @return The saved auth info.
    */
  def save(
    loginInfo: silhouette.api.LoginInfo,
    authInfo: silhouette.impl.providers.OAuth1Info
  ): Future[silhouette.impl.providers.OAuth1Info]

  /**
    * Removes the auth info for the given login info.
    *
    * @param loginInfo The login info for which the auth info should be removed.
    * @return A future to wait for the process to be completed.
    */
  def remove(loginInfo: silhouette.api.LoginInfo): Future[Unit]
}

/** Provides DAO interface for data access to the
  * [[OAuth1InfoDAOInterface.OAuth1InfoDAOImpl.tableQuery oauth1 info]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[OAuth1InfoDAO]] is accessed by mixing in
  * [[OAuth1InfoDAOInterface]]
  * {{{
  * class Foo(id: Long) extends OAuth1InfoDAOInterface {
  *   def fooFn: OAuth1Info = oAuth1InfoDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[OAuth1InfoTable]]
  * {{{
  * class Foo() extends OAuth1InfoDAOInterface {
  *   val tableQuery: TableQuery[OAuth1InfoTable] = TableQuery[OAuth1InfoTable]
  * }
  * }}}
  */
trait OAuth1InfoDAOInterface extends DAOHelpers
  with LoginInfoDAOInterface {
  import profile.api._
  import com.softwaremill.macwire._
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The oauth1 info data access implementation instance. */
  lazy val oAuth1InfoDAO: OAuth1InfoDAO = wire[OAuth1InfoDAOImpl]

  /** Implementation of data access methods associated with the
    * [[OAuth1InfoDAOImpl.tableQuery oauth1 info]] database table.
    */
  class OAuth1InfoDAOImpl() extends OAuth1InfoDAO with CRUDImpl[
    OAuth1InfoTable, OAuth1Info
  ] {

    /** The oauth1 info database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[OAuth1InfoTable]

    def find(
      loginInfo: silhouette.api.LoginInfo
    ): Future[Option[silhouette.impl.providers.OAuth1Info]] = {
      val result = db.run(_oAuth1InfoQuery(loginInfo).result.headOption)
      result.map { dbOAuth1InfoOption => dbOAuth1InfoOption.map(
        dbOAuth1Info => silhouette.impl.providers.OAuth1Info(
          dbOAuth1Info.token, dbOAuth1Info.secret
        )
      )}
    }

    def add(
      loginInfo: silhouette.api.LoginInfo, authInfo: silhouette.impl.providers.OAuth1Info
    ): Future[silhouette.impl.providers.OAuth1Info] = db.run(
      _addAction(loginInfo, authInfo)
    ).map(_ => authInfo)

    def update(
      loginInfo: silhouette.api.LoginInfo, authInfo: silhouette.impl.providers.OAuth1Info
    ): Future[silhouette.impl.providers.OAuth1Info] = db.run(
      _updateAction(loginInfo = loginInfo, authInfo = authInfo)
    ).map(_ => authInfo)

    def save(
      loginInfo: silhouette.api.LoginInfo,
      authInfo: silhouette.impl.providers.OAuth1Info
    ): Future[silhouette.impl.providers.OAuth1Info] = {
      val query = loginInfoDAO.getLoginInfos(
        loginInfo = loginInfo
      ).joinLeft(tableQuery).on(_.id === _.loginInfoId)
      val action = query.result.head.flatMap {
        case (dbLoginInfo, Some(dbOAuth1Info)) => _updateAction(
          loginInfo = loginInfo, authInfo = authInfo
        )
        case (dbLoginInfo, None) => _addAction(loginInfo = loginInfo, authInfo = authInfo)
      }.transactionally
      db.run(action).map(_ => authInfo)
    }

    def remove(loginInfo: silhouette.api.LoginInfo): Future[Unit] =
      db.run(_oAuth1InfoSubQuery(loginInfo).delete).map(_ => ())

    private def _oAuth1InfoQuery(loginInfo: silhouette.api.LoginInfo) = for {
      dbLoginInfo <- loginInfoDAO.getLoginInfos(loginInfo = loginInfo)
      dbOAuth1Info <- tableQuery if dbOAuth1Info.loginInfoId === dbLoginInfo.id
    } yield dbOAuth1Info

    // Use subquery workaround instead of join to get authinfo because slick only
    // supports selecting from a single table for update/delete queries
    // (https://github.com/slick/slick/issues/684).
    private def _oAuth1InfoSubQuery(loginInfo: silhouette.api.LoginInfo) =
      tableQuery.filter(
        _.loginInfoId in loginInfoDAO.getLoginInfos(loginInfo = loginInfo).map(_.id)
      )

    private def _addAction(
      loginInfo: silhouette.api.LoginInfo, authInfo: silhouette.impl.providers.OAuth1Info
    ) = loginInfoDAO.getLoginInfo(loginInfo = loginInfo).flatMap { dbLoginInfo =>
      tableQuery += OAuth1Info(
        id = None,
        loginInfoId = dbLoginInfo.id.get,
        secret = authInfo.secret,
        token = authInfo.token
      )
    }.transactionally

    private def _updateAction(
      loginInfo: silhouette.api.LoginInfo, authInfo: silhouette.impl.providers.OAuth1Info
    ) = _oAuth1InfoSubQuery(loginInfo).map(
      dbOAuthInfo => (dbOAuthInfo.token, dbOAuthInfo.secret)
    ).update((authInfo.token, authInfo.secret))
  }

  /** The oauth1 info database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class OAuth1InfoTable(tag: Tag) extends RichTable[OAuth1Info](
    tag = tag, name = "OAUTH1_INFO"
  ) {
    def loginInfoId = column[Long]("LOGIN_INFO_ID", DBType("BIGINT NOT NULL"))

    def secret = column[String]("ACCESS_SECRET", O.Length(length = 512, varying = true))

    def token = column[String]("ACCESS_TOKEN", O.Length(length = 512, varying = true))

    def * = (
      id.?, loginInfoId, secret, token
    ) <> ((OAuth1Info.apply _).tupled, OAuth1Info.unapply)

    def ? = (
      id.?, loginInfoId, secret, token
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (OAuth1Info.apply _).tupled(
            (_1, _2, _3, _4)
          ))
      },
      maybeUnapply
    )
  }
}
