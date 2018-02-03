package aurita.controllers.auth.daos

import scala.concurrent.ExecutionContext
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import aurita.daos.auth.{
  UserDAO,
  UserXGroupDAO,
  UserTokenDAO,
  UserDAOInterface,
  UserXGroupDAOInterface,
  UserTokenDAOInterface
}

abstract class AuthControllerDAO {
  val userTokenDAO: UserTokenDAO
  val userDAO: UserDAO
  val userXGroupDAO: UserXGroupDAO
}

class AuthControllerDAOImpl(
  protected val dbConfig: DatabaseConfig[JdbcProfile]
)(implicit val executionContext: ExecutionContext) extends AuthControllerDAO
  with UserXGroupDAOInterface
  with UserTokenDAOInterface
  with UserDAOInterface {}
