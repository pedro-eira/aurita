package aurita.daos.auth

import com.mohiva.play.silhouette
import silhouette.persistence.daos.DelegableAuthInfoDAO
import scala.concurrent.Future
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.PasswordInfo

/** Provide the declaration of data access methods associated with the
  * [[PasswordInfoDAOInterface.PasswordInfoDAOImpl.tableQuery passwordInfo]] database table.
  *
  * The methods are defined in
  * [[PasswordInfoDAOInterface.PasswordInfoDAOImpl PasswordInfoDAOImpl]].
  *
  */
trait PasswordInfoDAO extends DelegableAuthInfoDAO[silhouette.api.util.PasswordInfo]
  with CRUD[PasswordInfo] {}

/** Provides DAO interface for data access to the
  * [[PasswordInfoDAOInterface.PasswordInfoDAOImpl.tableQuery password info]]
  * database table.
  *
  * ==Overview==
  * The implemented instance of [[PasswordInfoDAO]] is accessed by mixing in
  * [[PasswordInfoDAOInterface]]
  * {{{
  * class Foo(id: Long) extends PasswordInfoDAOInterface {
  *   def fooFn: PasswordInfo = passwordInfoDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[PasswordInfoTable]]
  * {{{
  * class Foo() extends PasswordInfoDAOInterface {
  *   val tableQuery: TableQuery[PasswordInfoTable] = TableQuery[PasswordInfoTable]
  * }
  * }}}
  */
trait PasswordInfoDAOInterface extends DAOHelpers
  with LoginInfoDAOInterface {
  import profile.api._
  import com.softwaremill.macwire._
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The password info data access implementation instance. */
  lazy val passwordInfoDAO: PasswordInfoDAO = wire[PasswordInfoDAOImpl]

  /** Implementation of data access methods associated with the
    * [[PasswordInfoDAOImpl.tableQuery password info]] database table.
    */
  class PasswordInfoDAOImpl() extends PasswordInfoDAO with CRUDImpl[
    PasswordInfoTable, PasswordInfo
  ] {

    /** The password info database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[PasswordInfoTable]
    protected val loginInfoTableQuery = TableQuery[LoginInfoTable]

    /**
     * Adds new auth info for the given login info.
     *
     * @param loginInfo The login info for which the auth info should be added.
     * @param authInfo The auth info to add.
     * @return The added auth info.
     */
    def add(
      loginInfo: silhouette.api.LoginInfo,
      authInfo: silhouette.api.util.PasswordInfo
    ): Future[silhouette.api.util.PasswordInfo] = db.run(
      (for {
        liIdo <- (for {
          li <- loginInfoTableQuery.filter(
            t => t.providerId === loginInfo.providerID &&
              t.providerKey === loginInfo.providerKey
          )
        } yield li.id).result.headOption flatMap ( r => r match {
          case Some(liIdo0) => slick.dbio.DBIOAction.successful(liIdo0)
          case None => slick.dbio.DBIOAction.failed(
            new Exception(s"Login Info not found: ${loginInfo}")
          )
        } )
        pi <- (
          tableQuery returning tableQuery.map(_.id)
            into ((ans, id) => ans.copy(id = Option(id)))
        ) += PasswordInfo(
          id = None,
          loginInfoId = liIdo,
          hasher = authInfo.hasher,
          password = authInfo.password,
          salt = authInfo.salt
        )
      } yield silhouette.api.util.PasswordInfo(
        hasher = pi.hasher,
        password = pi.password,
        salt = pi.salt
      ))
    )

    /**
     * Finds the auth info which is linked with the specified login info.
     *
     * @param loginInfo The linked login info.
     * @return The retrieved auth info or None if no auth info could be
     * retrieved for the given login info.
     */
    def find(
      loginInfo: silhouette.api.LoginInfo
    ): Future[Option[silhouette.api.util.PasswordInfo]] =
      db.run(
        (for {
          (pi, li) <- tableQuery join loginInfoTableQuery on (
            _.loginInfoId === _.id
          ) if li.providerId === loginInfo.providerID &&
            li.providerKey === loginInfo.providerKey
        } yield pi).result.headOption map {
          case Some(pi) => Option(silhouette.api.util.PasswordInfo(
            hasher = pi.hasher,
            password = pi.password,
            salt = pi.salt
          ))
          case None => None
        }
      )

    /**
     * Updates the auth info for the given login info.
     *
     * @param loginInfo The login info for which the auth info should be updated.
     * @param authInfo The auth info to update.
     * @return The updated auth info.
     */
    def update(
      loginInfo: silhouette.api.LoginInfo,
      authInfo: silhouette.api.util.PasswordInfo
    ): Future[silhouette.api.util.PasswordInfo] = db.run(
      (for {
        pio <- (for {
          (pi, li) <- tableQuery join loginInfoTableQuery on (
            _.loginInfoId === _.id
          ) if li.providerId === loginInfo.providerID &&
            li.providerKey === loginInfo.providerKey
        } yield pi).result.headOption
        rowsAffected <- {
          val pi = pio.get
          tableQuery.filter(t => t.id === pi.id.get).update(pi.copy(
            hasher = authInfo.hasher, password = authInfo.password, salt = authInfo.salt
          ))
        }
      } yield rowsAffected).map(_ => authInfo)
    )

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
      authInfo: silhouette.api.util.PasswordInfo
    ): Future[silhouette.api.util.PasswordInfo] = db.run(
      (for {
        liido <- (for {
          li <- loginInfoTableQuery.filter(
            t => t.providerId === loginInfo.providerID &&
              t.providerKey === loginInfo.providerKey
          )
        } yield li.id).result.headOption
        pio <- (for {
          pi2 <- tableQuery.filter(
            t => t.loginInfoId === liido.get
          )
        } yield pi2).result.headOption
        pi <- {
          val pi3: PasswordInfo = pio match {
            case Some(p) => p.copy(
              hasher = authInfo.hasher, password = authInfo.password, salt = authInfo.salt
            )
            case None => PasswordInfo(
              id = None,
              loginInfoId = liido.get,
              hasher = authInfo.hasher,
              password = authInfo.password,
              salt = authInfo.salt
            )
          }
          (tableQuery returning tableQuery.map(_.id)).insertOrUpdate(pi3)
        }
      } yield pi).map(_ => authInfo)
    )

    /**
     * Removes the auth info for the given login info.
     *
     * @param loginInfo The login info for which the auth info should be removed.
     * @return A future to wait for the process to be completed.
     */
    def remove(loginInfo: silhouette.api.LoginInfo): Future[Unit] = db.run({
      val q = loginInfoTableQuery.filter(
        t => t.providerId === loginInfo.providerID &&
          t.providerKey === loginInfo.providerKey
      )
      tableQuery.filter(_.loginInfoId in q.map(_.id)).delete map(_ => ())
    })

  }

  /** The password info database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class PasswordInfoTable(tag: Tag) extends RichTable[PasswordInfo](
    tag = tag, name = "PASSWORD_INFO"
  ) {
    def loginInfoId = column[Long]("LOGIN_INFO_ID", DBType("BIGINT NOT NULL"))

    def hasher =
      column[String]("HASHER", O.Length(length = 254, varying = true))

    def password =
      column[String]("PASSWORD", O.Length(length = 254, varying = true))

    def salt =
      column[Option[String]]("SALT", O.Length(length = 254, varying = true))

    def loginInfoFK =
      foreignKey(
        "PINFO_LOGIN_INFO_FK", loginInfoId, TableQuery[LoginInfoTable]
      )(_.id)

    def loginInfoIdUC =
      index("PINFO_LOGIN_INFO_ID_UC", loginInfoId, unique = true)

    def * = (
      id.?,
      loginInfoId,
      hasher,
      password,
      salt
    ) <> ((PasswordInfo.apply _).tupled, PasswordInfo.unapply)

    def ? = (
      id.?,
      loginInfoId,
      hasher,
      password,
      salt
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (PasswordInfo.apply _).tupled(
            (_1, _2, _3, _4, _5)
          ))
      },
      maybeUnapply
    )
  }
}
