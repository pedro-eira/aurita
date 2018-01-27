package aurita.test.daos.auth

import java.util.UUID
import org.junit.runner.RunWith
import org.specs2.execute._
import org.specs2.runner.JUnitRunner
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.Random
import aurita.models.auth.UserToken
import aurita.daos.utility.DAOHelpersExceptions
import aurita.test.daos.utility.TestEnvironment
import java.sql.Timestamp

trait UserTokenDAOSpecHelper extends TestEnvironment {
  import aurita.daos.auth.UserTokenDAOInterface
  import scala.concurrent.ExecutionContext

  trait TestData extends UserTokenDAOInterface {
    import profile.api._
    import slick.basic.BasicAction
    import slick.dbio.{ NoStream, Effect }
    import aurita.models.CurrentStatus
    import aurita.models.auth.{ User, UserGroup, UserRole }

    type MyBasicAction = BasicAction[UserToken, NoStream, Effect.Write]

    protected val tableQuery = TableQuery[UserTokenTable]
    protected val roleTableQuery = TableQuery[UserRoleTable]
    protected val groupTableQuery = TableQuery[UserGroupTable]
    protected val userTableQuery = TableQuery[UserTable]
    protected val statusTableQuery = TableQuery[CurrentStatusTable]

    lazy val expiresOn: Long = System.currentTimeMillis + 100000

    lazy val userToken: UserToken =
      UserToken(
        id = None, userId = 1, tokenId = "SS534gfe", expiresOn = new Timestamp(expiresOn)
      )

    lazy val user0: User =
      User(
        id = Some(1),
        groupId = 1,
        roleId = 1,
        statusId = 1,
        userId = UUID.randomUUID().toString(),
        avatarURL = None,
        email = "foo@titusi.com",
        firstName = Some("Foo"),
        lastName = Some("Pillock"),
        username = "foo",
        usernameSuffix = None
      )

    lazy val user1: User =
      User(
        id = Some(2),
        groupId = 2,
        roleId = 1,
        statusId = 1,
        userId = UUID.randomUUID().toString(),
        avatarURL = None,
        email = "foo2@titusi.com",
        firstName = Some("Foo"),
        lastName = Some("Pillock"),
        username = "foo2",
        usernameSuffix = Option(22)
      )

    lazy val group0: UserGroup = UserGroup(
      id = Some(1),
      active = true,
      description = "NA",
      name = "dummy"
    )

    lazy val group1: UserGroup = UserGroup(
      id = Some(2),
      active = true,
      description = "NA some",
      name = "dummy2"
    )

    lazy val role: UserRole = UserRole(
      id = None,
      active = true,
      description = "A test role",
      name = "fooRole",
      selectable = true
    )

    lazy val status: CurrentStatus = CurrentStatus(
      id = None,
      active = true,
      description = "A test status",
      key = "user",
      value = "fooStatus"
    )

    def init = {
      Await.result(
        db.run(
          (roleTableQuery returning roleTableQuery.map(_.id)).insertOrUpdate(
            role
          )
        ), Duration.Inf
      )
      Await.result(
        db.run(
          (groupTableQuery returning groupTableQuery.map(_.id)).insertOrUpdate(
            group0
          )
        ), Duration.Inf
      )
      Await.result(
        db.run(
          (groupTableQuery returning groupTableQuery.map(_.id)).insertOrUpdate(
            group1
          )
        ), Duration.Inf
      )
      Await.result(
        db.run(
          (statusTableQuery returning statusTableQuery.map(_.id)).insertOrUpdate(
            status
          )
        ), Duration.Inf
      )
      Await.result(
        db.run(
          (userTableQuery returning userTableQuery.map(_.id)).insertOrUpdate(
            user0
          )
        ), Duration.Inf
      )
      Await.result(
        db.run(
          (userTableQuery returning userTableQuery.map(_.id)).insertOrUpdate(
            user1
          )
        ), Duration.Inf
      )
    }

    private def _insert(model: UserToken): MyBasicAction =
      (tableQuery returning tableQuery.map(_.id)
        into ((ans, id) => ans.copy(id = Option(id)))
      ) += model

   /**
    * Delete a specific entity by id. If successfully completed return true,
    * else false
    */
    def delete(id: Long): Future[Int] =
      db.run(tableQuery.filter(t => t.id === id).delete)

   /**
    * Find a specific entity by id.
    */
    def findById(id: Long): Future[Option[UserToken]] =
      db.run(tableQuery.filter(t => t.id === id).result.headOption)

    def insert(model: UserToken): Future[UserToken] = db.run(_insert(model))

    def update(model: UserToken): Future[Int] = db.run(
      tableQuery.filter(t => t.id === model.id).update(model)
    )

    def insertOrUpdate(model: UserToken): Future[UserToken] = db.run(
      (tableQuery returning tableQuery.map(_.id)).insertOrUpdate(model)
    ) map { _ match { 
      case None => model
      case Some(id) => model.copy(id = Option(id))
    } }
  }

  implicit val ec: ExecutionContext = ExecutionContext.global

  class WithDepsApplication()(
    implicit val ec: ExecutionContext
  ) extends TestApplication with TestData
}

@RunWith(classOf[JUnitRunner])
object UserTokenDAOInsertSpec extends UserTokenDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException
  import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation

  "An insert of a unique user token" should {
    "successfully return the inserted userToken" in new WithDepsApplication {
      init
      Await.result(insert(userToken), Duration.Inf) must
        beEqualTo(userToken.copy(id = Some(1)))
      val userToken2: UserToken = userToken.copy(userId = 2)
      Await.result(insert(userToken2), Duration.Inf) must
        beEqualTo(userToken2.copy(id = Some(2)))
    }
  }
  "An insert of a non-unique user token" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(insert(userToken), Duration.Inf)
      Await.result(insert(userToken), Duration.Inf) must
        throwAn[SQLIntegrityConstraintViolationException]
    }
  }
  "An insert of a user token with an errorneous token id value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insert(userToken.copy(tokenId = null)), Duration.Inf
      ) must throwAn[Exception]
    }
  }
  "An insert of a userToken with a 64 char token id value" should {
    "successfully return the inserted user token" in
      new WithDepsApplication {
      init
      val userToken2: UserToken = userToken.copy(
        tokenId = Random.nextString(64)
      )
      Await.result(insert(userToken2), Duration.Inf) must
        beEqualTo(userToken2.copy(id = Some(1)))
    }
  }
  "An insert of a user token with a 65 char token id value" should {
    "return a failure" in new WithDepsApplication {
      init
      val userToken2: UserToken =
        userToken.copy(tokenId = Random.nextString(65))
      Await.result(insert(userToken2), Duration.Inf) must
        throwAn[MysqlDataTruncation]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserTokenDAOInsertOrUpdateSpec extends UserTokenDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException

  "An insert or update of an existing user" should {
    "successfully return the saved user" in new WithDepsApplication {
      init
      val insertedUserToken: UserToken = userToken.copy(id = Some(1))
      Await.result(insert(userToken), Duration.Inf) must
        beEqualTo(insertedUserToken)
      val userToken2 =
        insertedUserToken.copy(expiresOn = new Timestamp(expiresOn + 2000))
      Await.result(insertOrUpdate(userToken2), Duration.Inf) must
        beEqualTo(userToken2)
    }
  }

  "An insert or update of a non-existing user" should {
    "successfully return the saved user" in new WithDepsApplication {
      init
      Await.result(insertOrUpdate(userToken), Duration.Inf) must
        beEqualTo(userToken.copy(id = Some(1)))
    }
  }

  "An insert or update of a user token with an errorneous value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(userToken.copy(tokenId = null)), Duration.Inf
      ) must throwAn[
        SQLIntegrityConstraintViolationException
      ]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserTokenDAOFindByIdSpec extends UserTokenDAOSpecHelper {
  "A search of an existing user token by its id" should {
    "successfully return the user token" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserToken: UserToken = userToken.copy(id = Some(id))
      Await.result(insert(userToken), Duration.Inf) must
        beEqualTo(insertedUserToken)
      Await.result(findById(id), Duration.Inf) must
        beEqualTo(Some(insertedUserToken))
    }
  }
  "A search of a non-existing user token by its id" should {
    "successfully return no user token" in new WithDepsApplication {
      init
      Await.result(findById(1), Duration.Inf) must beEqualTo(None)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserTokenDAODeleteSpec extends UserTokenDAOSpecHelper {
  "The delete of an existing user token by its id" should {
    "return the number of user tokens deleted (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserToken: UserToken = userToken.copy(id = Some(id))
      Await.result(insert(userToken), Duration.Inf) must
        beEqualTo(insertedUserToken)
      Await.result(delete(id), Duration.Inf) must beEqualTo(1)
    }
  }
  "The delete of a non-existing user token by its id" should {
    "return 0 user tokens deleted" in new WithDepsApplication {
      init
      Await.result(delete(1), Duration.Inf) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserTokenDAOUpdateSpec extends UserTokenDAOSpecHelper {
  "The update of an existing user token" should {
    "return the number of user tokens updated (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserToken: UserToken = userToken.copy(id = Some(id))
      Await.result(insert(userToken), Duration.Inf) must
        beEqualTo(insertedUserToken)
      Await.result(
        update(insertedUserToken.copy(tokenId = "foo2")),
        Duration.Inf
      ) must beEqualTo(1)
    }
  }
  "The update of a non-existing user token with an id" should {
    "return 0 user tokens updated" in new WithDepsApplication {
      init
      Await.result(
        update(userToken.copy(id = Some(1))), Duration.Inf
      ) must beEqualTo(0)
    }
  }
  "The update of a non-existing user token with no id" should {
    "return 0 user tokens updated" in new WithDepsApplication {
      init
      Await.result(
        update(userToken), Duration.Inf
      ) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserTokenDAODeleteByTokenIdSpec extends UserTokenDAOSpecHelper {

  "The delete of an existing user token by its token id" should {
    "return the number of user tokens deleted (1)" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserToken: UserToken = userToken.copy(id = Some(id))
      Await.result(insert(userToken), Duration.Inf) must
        beEqualTo(insertedUserToken)
      Await.result(userTokenDAO.deleteByTokenId(userToken.tokenId), Duration.Inf) must
        beEqualTo(1)
    }
  }
  "The delete of a non-existing user token by its token id" should {
    "return 0 user tokens deleted" in new WithDepsApplication {
      init
      Await.result(userTokenDAO.deleteByTokenId("unknown"), Duration.Inf) must
        beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserTokenDAOFindByTokenIdSpec extends UserTokenDAOSpecHelper {

  "A search of an existing user token by its token id" should {
    "successfully return the user" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserToken: UserToken = userToken.copy(id = Some(id))
      Await.result(insert(userToken), Duration.Inf) must
        beEqualTo(insertedUserToken)
      Await.result(userTokenDAO.findByTokenId(userToken.tokenId), Duration.Inf) must
        beEqualTo(Some(insertedUserToken))
    }
  }
  "A search of a non-existing user token by its token id" should {
    "successfully return no user" in new WithDepsApplication {
      init
      Await.result(userTokenDAO.findByTokenId("unknown"), Duration.Inf) must
        beEqualTo(None)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserTokenDAOFindByUserIdSpec extends UserTokenDAOSpecHelper {

  "A search of an existing user token by its user id" should {
    "successfully return the user" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserToken: UserToken = userToken.copy(id = Some(id))
      Await.result(insert(userToken), Duration.Inf) must
        beEqualTo(insertedUserToken)
      Await.result(userTokenDAO.findByUserId(userToken.userId), Duration.Inf) must
        beEqualTo(Some(insertedUserToken))
    }
  }
  "A search of a non-existing user token by its user id" should {
    "successfully return no user" in new WithDepsApplication {
      init
      Await.result(userTokenDAO.findByUserId(999), Duration.Inf) must
        beEqualTo(None)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserTokenDAOParentSpec extends UserTokenDAOSpecHelper {
  override def is = s2"""
    These are the full User Token DAO specs
    ${"child0" ~ UserTokenDAOInsertSpec}
    ${"child1" ~ UserTokenDAOInsertOrUpdateSpec}
    ${"child2" ~ UserTokenDAOFindByIdSpec}
    ${"child3" ~ UserTokenDAODeleteSpec}
    ${"child4" ~ UserTokenDAOUpdateSpec}
    ${"child5" ~ UserTokenDAOFindByTokenIdSpec}
    ${"child6" ~ UserTokenDAOFindByUserIdSpec}
    ${"child7" ~ UserTokenDAODeleteByTokenIdSpec}
  """
}
