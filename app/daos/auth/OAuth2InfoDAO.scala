package aurita.daos.auth

import com.mohiva.play.silhouette
import silhouette.persistence.daos.DelegableAuthInfoDAO
import scala.concurrent.Future
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.OAuth2Info

/** Provide the declaration of data access methods associated with the
  * [[OAuth2InfoDAOInterface.OAuth2InfoDAOImpl.tableQuery oauth2Info]] database table.
  *
  * The methods are defined in
  * [[OAuth2InfoDAOInterface.OAuth2InfoDAOImpl OAuth2InfoDAOImpl]].
  *
  */
trait OAuth2InfoDAO extends DelegableAuthInfoDAO[silhouette.impl.providers.OAuth2Info]
  with CRUD[OAuth2Info] {
    /**
      * Finds the auth info which is linked with the specified login info.
      *
      * @param loginInfo The linked login info.
      * @return The retrieved auth info or None if no auth info could be retrieved
      * for the given login info.
      */
    def find(
      loginInfo: silhouette.api.LoginInfo
    ): Future[Option[silhouette.impl.providers.OAuth2Info]]

    /**
      * Adds new auth info for the given login info.
      *
      * @param loginInfo The login info for which the auth info should be added.
      * @param authInfo The auth info to add.
      * @return The added auth info.
      */
    def add(
      loginInfo: silhouette.api.LoginInfo,
      authInfo: silhouette.impl.providers.OAuth2Info
    ): Future[silhouette.impl.providers.OAuth2Info]

    /**
      * Updates the auth info for the given login info.
      *
      * @param loginInfo The login info for which the auth info should be updated.
      * @param authInfo The auth info to update.
      * @return The updated auth info.
      */
    def update(
      loginInfo: silhouette.api.LoginInfo,
      authInfo: silhouette.impl.providers.OAuth2Info
    ): Future[silhouette.impl.providers.OAuth2Info]

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
      authInfo: silhouette.impl.providers.OAuth2Info
    ): Future[silhouette.impl.providers.OAuth2Info]

    /**
      * Removes the auth info for the given login info.
      *
      * @param loginInfo The login info for which the auth info should be removed.
      * @return A future to wait for the process to be completed.
      */
    def remove(loginInfo: silhouette.api.LoginInfo): Future[Unit]
}

/** Provides DAO interface for data access to the
  * [[OAuth2InfoDAOInterface.OAuth2InfoDAOImpl.tableQuery oauth2 info]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[OAuth2InfoDAO]] is accessed by mixing in
  * [[OAuth2InfoDAOInterface]]
  * {{{
  * class Foo(id: Long) extends OAuth2InfoDAOInterface {
  *   def fooFn: OAuth2Info = oAuth2InfoDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[OAuth2InfoTable]]
  * {{{
  * class Foo() extends OAuth2InfoDAOInterface {
  *   val tableQuery: TableQuery[OAuth2InfoTable] = TableQuery[OAuth2InfoTable]
  * }
  * }}}
  */
trait OAuth2InfoDAOInterface extends DAOHelpers
  with LoginInfoDAOInterface {
  import profile.api._
  import com.softwaremill.macwire._
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The oauth2 info data access implementation instance. */
  lazy val oAuth2InfoDAO: OAuth2InfoDAO = wire[OAuth2InfoDAOImpl]

  /** Implementation of data access methods associated with the
    * [[OAuth2InfoDAOImpl.tableQuery oauth2 info]] database table.
    */
  class OAuth2InfoDAOImpl() extends OAuth2InfoDAO with CRUDImpl[
    OAuth2InfoTable, OAuth2Info
  ] {

    /** The oauth2 info database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[OAuth2InfoTable]

    def find(
      loginInfo: silhouette.api.LoginInfo
    ): Future[Option[silhouette.impl.providers.OAuth2Info]] = {
      val result = db.run(_oAuth2InfoQuery(loginInfo).result.headOption)
      result.map { dbOAuth2InfoOption =>
        dbOAuth2InfoOption.map { dbOAuth2Info =>
          silhouette.impl.providers.OAuth2Info(
            accessToken = dbOAuth2Info.accessToken,
            tokenType = dbOAuth2Info.tokenType,
            expiresIn = dbOAuth2Info.expiresIn,
            refreshToken = dbOAuth2Info.refreshToken
          )
        }
      }
    }

    def add(
      loginInfo: silhouette.api.LoginInfo,
      authInfo: silhouette.impl.providers.OAuth2Info
    ): Future[silhouette.impl.providers.OAuth2Info] = db.run(
      _addAction(loginInfo = loginInfo, authInfo = authInfo)
    ).map(_ => authInfo)

    def update(
      loginInfo: silhouette.api.LoginInfo,
      authInfo: silhouette.impl.providers.OAuth2Info
    ): Future[silhouette.impl.providers.OAuth2Info] = db.run(
      _updateAction(loginInfo, authInfo)
    ).map(_ => authInfo)

    def save(
      loginInfo: silhouette.api.LoginInfo,
      authInfo: silhouette.impl.providers.OAuth2Info
    ): Future[silhouette.impl.providers.OAuth2Info] = {
      val query = for {
        result <- loginInfoDAO.getLoginInfos(loginInfo = loginInfo).joinLeft(
          tableQuery
        ).on(_.id === _.loginInfoId)
      } yield result
      val action = query.result.head.flatMap {
        case (dbLoginInfo, Some(dbOAuth2Info)) => _updateAction(
          loginInfo = loginInfo, authInfo = authInfo
        )
        case (dbLoginInfo, None) => _addAction(loginInfo = loginInfo, authInfo = authInfo)
      }.transactionally
      db.run(action).map(_ => authInfo)
    }

    def remove(loginInfo: silhouette.api.LoginInfo): Future[Unit] =
      db.run(_oAuth2InfoSubQuery(loginInfo = loginInfo).delete).map(_ => ())

    private def _oAuth2InfoQuery(loginInfo: silhouette.api.LoginInfo) = for {
      dbLoginInfo <- loginInfoDAO.getLoginInfos(loginInfo = loginInfo)
      dbOAuth2Info <- tableQuery if dbOAuth2Info.loginInfoId === dbLoginInfo.id
    } yield dbOAuth2Info

    // Use subquery workaround instead of join to get authinfo because slick only supports
    // selecting from a single table for update/delete queries
    // (https://github.com/slick/slick/issues/684).
    private def _oAuth2InfoSubQuery(loginInfo: silhouette.api.LoginInfo) = tableQuery.filter(
      _.loginInfoId in loginInfoDAO.getLoginInfos(loginInfo = loginInfo).map(_.id)
    )

    private def _addAction(
      loginInfo: silhouette.api.LoginInfo, authInfo: silhouette.impl.providers.OAuth2Info
    ) = loginInfoDAO.getLoginInfo(loginInfo = loginInfo).flatMap { dbLoginInfo =>
      tableQuery += OAuth2Info(
        id = None,
        loginInfoId = dbLoginInfo.id.get,
        accessToken = authInfo.accessToken,
        expiresIn = authInfo.expiresIn,
        refreshToken = authInfo.refreshToken,
        tokenType = authInfo.tokenType
      )
    }.transactionally

    private def _updateAction(
      loginInfo: silhouette.api.LoginInfo,
      authInfo: silhouette.impl.providers.OAuth2Info
    ) = _oAuth2InfoSubQuery(loginInfo = loginInfo).map(
      dbOAuth2Info => (
        dbOAuth2Info.accessToken,
        dbOAuth2Info.tokenType,
        dbOAuth2Info.expiresIn,
        dbOAuth2Info.refreshToken
      )
    ).update(
      (
        authInfo.accessToken,
        authInfo.tokenType,
        authInfo.expiresIn,
        authInfo.refreshToken
      )
    )
  }

  /** The oauth2 info database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class OAuth2InfoTable(tag: Tag) extends RichTable[OAuth2Info](
    tag = tag, name = "OAUTH2_INFO"
  ) {
    def loginInfoId = column[Long]("LOGIN_INFO_ID", DBType("BIGINT NOT NULL"))

    def accessToken =
      column[String]("ACCESS_TOKEN", O.Length(length = 512, varying = true))

    def expiresIn = column[Option[Int]]("EXPIRES_IN", DBType("INT UNSIGNED"))

    def refreshToken =
      column[Option[String]]("REFRESH_TOKEN", O.Length(length = 512, varying = true))

    def tokenType =
      column[Option[String]]("TOKEN_TYPE", O.Length(length = 512, varying = true))

    def * = (
      id.?,
      loginInfoId,
      accessToken,
      expiresIn,
      refreshToken,
      tokenType
    ) <> ((OAuth2Info.apply _).tupled, OAuth2Info.unapply)

    def ? = (
      id.?,
      loginInfoId,
      accessToken,
      expiresIn,
      refreshToken,
      tokenType
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (OAuth2Info.apply _).tupled(
            (_1, _2, _3, _4, _5, _6)
          ))
      },
      maybeUnapply
    )
  }
}
