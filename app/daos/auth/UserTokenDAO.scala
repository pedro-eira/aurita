package aurita.daos.auth

import scala.concurrent.Future
import aurita.daos.utility.{ DAOHelpers, CRUD }
import aurita.models.auth.UserToken
import aurita.models.auth.utility.ResetPasswordData

/** Provide the declaration of data access methods associated with the
  * [[UserTokenDAOInterface.UserTokenDAOImpl.tableQuery user token]] database table.
  *
  * The methods are defined in
  * [[UserTokenDAOInterface.UserTokenDAOImpl UserTokenDAOImpl]].
  *
  */
trait UserTokenDAO extends CRUD[UserToken] {

   /** Find a specific [[aurita.models.auth.UserToken user token]] by its token id.
    *
    * @param tokenId the token id associated with the user token.
    *
    */
  def findByTokenId(tokenId: String): Future[Option[UserToken]]

   /** Insert a [[aurita.models.auth.UserToken user token]].
    *
    * @param token the token to insert.
    *
    */
  def insert(token: UserToken): Future[UserToken]

   /** Insert or update a [[aurita.models.auth.UserToken user token]].
    *
    * @param token the token to upsert.
    *
    */
  def upsert(token: UserToken): Future[UserToken]

   /** Get a user [[aurita.models.auth.auth.utility.ResetPasswordData reset password data]].
    *
    * @param userId the user id associated with the requested reset password data.
    *
    */
  def getResetPasswordDataByUserId(userId: Long): Future[Option[ResetPasswordData]]

   /**
    * Find a specific [[aurita.models.auth.UserToken user token]] by its user id.
    *
    * @param userId the user id associated with the [[aurita.models.auth.UserToken user token]].
    *
    */
  def findByUserId(userId: Long): Future[Option[UserToken]]

   /** Delete a specific [[aurita.models.auth.UserToken user token]].
    *
    * @param tokenId the token id associated with the user token.
    *
    */
  def deleteByTokenId(tokenId: String): Future[Int]
}

/** Provides DAO interface for data access to the
  * [[UserTokenDAOInterface.UserTokenDAOImpl.tableQuery user token]] database table.
  *
  * ==Overview==
  * The implemented instance of [[UserTokenDAO]] is accessed by mixing in
  * [[UserTokenDAOInterface]]
  * {{{
  * class Foo(id: Long) extends UserTokenDAOInterface {
  *   def fooFn: UserToken = userTokenDAO.findById(id)
  * }
  * }}}
  *
  * You can also access [[UserTokenTable]]
  * {{{
  * class Foo() extends UserTokenDAOInterface {
  *   val tableQuery: TableQuery[UserTokenTable] = TableQuery[UserTokenTable]
  * }
  * }}}
  */
trait UserTokenDAOInterface extends DAOHelpers
  with UserXLoginInfoDAOInterface {
  import profile.api._
  import java.sql.Timestamp
  import com.softwaremill.macwire._
  import scala.concurrent.ExecutionContext

  implicit val executionContext: ExecutionContext

  /** The user token data access implementation instance. */
  lazy val userTokenDAO: UserTokenDAO = wire[UserTokenDAOImpl]

  /** Implementation of data access methods associated with the
    * [[UserTokenDAOImpl.tableQuery user token]] database table.
    */
  class UserTokenDAOImpl() extends UserTokenDAO with CRUDImpl[UserTokenTable, UserToken] {
    import com.mohiva.play.silhouette.api.LoginInfo

    /** The user token database table.
      * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table-query]]
      */
    protected val tableQuery = TableQuery[UserTokenTable]
    protected lazy val loginInfoTableQuery = TableQuery[LoginInfoTable]
    protected lazy val userXLoginInfoTableQuery = TableQuery[UserXLoginInfoTable]
    protected lazy val userTableQuery = TableQuery[UserTable]

    def findByTokenId(tokenId: String): Future[Option[UserToken]] =
      db.run(tableQuery.filter(t => t.tokenId === tokenId).result.headOption)

    def getResetPasswordDataByUserId(
      userId: Long
    ): Future[Option[ResetPasswordData]] =
      db.run(
        (for {
          (
            (
              (
                (ut, u),
                uli
              ),
              li
            )
          ) <- tableQuery join userTableQuery on (
            _.userId === _.id
          ) join userXLoginInfoTableQuery on (
            _._2.id === _.userId
          ) join loginInfoTableQuery on (
            _._2.loginInfoId === _.id
          ) if ut.userId === userId
        } yield (li, ut, u)).result.headOption.map {
          case Some(rpd) => Option(ResetPasswordData(
            loginInfo = LoginInfo(
              providerID = rpd._1.providerId, providerKey = rpd._1.providerKey
            ),
            token = rpd._2,
            user = rpd._3
          ))
          case None => None
        }
      )

    def insert(token: UserToken): Future[UserToken] = db.run((
      tableQuery returning tableQuery.map(_.id)
        into ((ans, id) => ans.copy(id = Option(id)))
    ) += token)

    def upsert(token: UserToken): Future[UserToken] = db.run(
      (tableQuery returning tableQuery.map(_.id)).insertOrUpdate(token)
    ) map {
      tokenOption => tokenOption match {
        case None => token
        case Some(id) => token.copy(id = Option(id))
      }
    }

    def findByUserId(userId: Long): Future[Option[UserToken]] =
      db.run(tableQuery.filter(t => t.userId === userId).result.headOption)

    def deleteByTokenId(tokenId: String): Future[Int] =
      db.run(tableQuery.filter(t => t.tokenId === tokenId).delete)
  }

  /** The user token database table row.
    * @see [[http://slick.typesafe.com/doc/3.1.0/schemas.html#table]]
    */
  class UserTokenTable(tag: Tag) extends RichTable[UserToken](
    tag = tag, name = "USER_TOKEN"
  ) {
    def userId = column[Long]("USER_ID", DBType("BIGINT NOT NULL"))

    def tokenId =
      column[String]("TOKEN_ID", O.Length(length = 64, varying = true))

    def expiresOn = column[Timestamp](
      "EXPIRES_ON", DBType("TIMESTAMP(3) NOT NULL")
    )

    def userIdUC = index("UTOKEN_USER_ID_UC", userId, unique = true)

    def userFK =
      foreignKey("UTOKEN_USER_FK", userId, TableQuery[UserTable])(_.id)

    def * = (
      id.?, userId, tokenId, expiresOn
    ) <> ((UserToken.apply _).tupled, UserToken.unapply)

    def ? = (
      id.?, userId, tokenId, expiresOn
    ).shaped <> (
      {
        r =>
          import r._
          _1.map(_ => (UserToken.apply _).tupled(
            (_1, _2, _3, _4))
          )
      },
      maybeUnapply
    )
  }
}
