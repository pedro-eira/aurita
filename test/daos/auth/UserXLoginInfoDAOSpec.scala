package aurita.test.daos.auth

import java.util.UUID
import com.mohiva.play.silhouette
import org.junit.runner.RunWith
import org.specs2.execute._
import org.specs2.runner.JUnitRunner
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.Random
import aurita.models.auth.UserXLoginInfo
import aurita.daos.utility.DAOHelpersExceptions
import aurita.test.daos.utility.TestEnvironment
import aurita.models.auth.{
  IdentityImpl, User, UserGroup, UserXGroup, UserReadable
}

trait UserXLoginInfoDAOSpecHelper extends TestEnvironment {
  import aurita.daos.auth.UserXLoginInfoDAOInterface
  import scala.concurrent.ExecutionContext

  trait TestData extends UserXLoginInfoDAOInterface {
    import profile.api._
    import slick.basic.BasicAction
    import slick.dbio.{ NoStream, Effect }
    import aurita.models.CurrentStatus
    import aurita.models.auth.{ UserRole, LoginInfo, UserPermission }

    type MyBasicAction = BasicAction[UserXLoginInfo, NoStream, Effect.Write]

    protected val tableQuery = TableQuery[UserXLoginInfoTable]
    protected val roleTableQuery = TableQuery[UserRoleTable]
    protected val groupTableQuery = TableQuery[UserGroupTable]
    protected val permissionTableQuery = TableQuery[UserPermissionTable]
    protected val userXGroupTableQuery = TableQuery[UserXGroupTable]
    protected val infoTableQuery = TableQuery[LoginInfoTable]
    protected val userTableQuery = TableQuery[UserTable]
    protected val statusTableQuery = TableQuery[CurrentStatusTable]

    lazy val userXLoginInfo: UserXLoginInfo =
      UserXLoginInfo(
        id = None, userId = 1, loginInfoId = 1
      )

    lazy val user: User =
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

    lazy val loginInfo0: LoginInfo = LoginInfo(
      id = Some(1), providerId = "Sfsf9898", providerKey = "SDSFD2342424234"
    )

    lazy val loginInfo1: LoginInfo = LoginInfo(
      id = Some(2), providerId = "ghjhg898", providerKey = "DHuiiu5464"
    )

    lazy val group: UserGroup = UserGroup(
      id = Some(1),
      active = true,
      description = "NA",
      name = "dummy"
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

    lazy val userPermission: UserPermission = UserPermission(
      id = Some(1),
      roleId = 1,
      active = true,
      read = true,
      write = true,
      execute = true
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
            group
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
            user
          )
        ), Duration.Inf
      )
      Await.result(
        db.run(
          (infoTableQuery returning infoTableQuery.map(_.id)).insertOrUpdate(
            loginInfo0
          )
        ), Duration.Inf
      )
      Await.result(
        db.run(
          (infoTableQuery returning infoTableQuery.map(_.id)).insertOrUpdate(
            loginInfo1
          )
        ), Duration.Inf
      )
      Await.result(
        db.run(
          (permissionTableQuery returning permissionTableQuery.map(_.id)).insertOrUpdate(
            userPermission
          )
        ), Duration.Inf
      )
    }

    private def _insert(model: UserXLoginInfo): MyBasicAction =
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
    def findById(id: Long): Future[Option[UserXLoginInfo]] =
      db.run(tableQuery.filter(t => t.id === id).result.headOption)

    def insert(model: UserXLoginInfo): Future[UserXLoginInfo] = db.run(_insert(model))

    def update(model: UserXLoginInfo): Future[Int] = db.run(
      tableQuery.filter(t => t.id === model.id).update(model)
    )

    def insertOrUpdate(model: UserXLoginInfo): Future[UserXLoginInfo] = db.run(
      (tableQuery returning tableQuery.map(_.id)).insertOrUpdate(model)
    ) map { _ match { 
      case None => model
      case Some(id) => model.copy(id = Option(id))
    } }

   /**
    * Find a specific [[aurita.models.auth.User user]] by id.
    */
    def userFindById(id: Long): Future[Option[User]] =
      db.run(userTableQuery.filter(t => t.id === id).result.headOption)

   /**
    * Find a specific [[aurita.models.auth.UserGroup user group]] by id.
    */
    def groupFindById(id: Long): Future[Option[UserGroup]] =
      db.run(groupTableQuery.filter(t => t.id === id).result.headOption)

   /**
    * Find a specific [[aurita.models.auth.UserXGroup user join group]] by id.
    */
    def userXGroupFindById(id: Long): Future[Option[UserXGroup]] =
      db.run(userXGroupTableQuery.filter(t => t.id === id).result.headOption)
  }

  implicit val ec: ExecutionContext = ExecutionContext.global

  class WithDepsApplication()(
    implicit val ec: ExecutionContext
  ) extends TestApplication with TestData
}

@RunWith(classOf[JUnitRunner])
object UserXLoginInfoDAOInsertSpec extends UserXLoginInfoDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException
  import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation

  "An insert of a unique user join login info" should {
    "successfully return the inserted userXLoginInfo" in new WithDepsApplication {
      init
      Await.result(insert(userXLoginInfo), Duration.Inf) must
        beEqualTo(userXLoginInfo.copy(id = Some(1)))
      val userXLoginInfo2: UserXLoginInfo = userXLoginInfo.copy(loginInfoId = 2)
      Await.result(insert(userXLoginInfo2), Duration.Inf) must
        beEqualTo(userXLoginInfo2.copy(id = Some(2)))
    }
  }
  "An insert of a non-unique user join login info" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(insert(userXLoginInfo), Duration.Inf)
      Await.result(insert(userXLoginInfo), Duration.Inf) must
        throwAn[SQLIntegrityConstraintViolationException]
    }
  }
  "An insert of a user join login info with an errorneous user id value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insert(userXLoginInfo.copy(userId = 3)), Duration.Inf
      ) must throwAn[Exception]
    }
  }
  "An insert of a user join login info with an errorneous login info id value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insert(userXLoginInfo.copy(loginInfoId = 4)), Duration.Inf
      ) must throwAn[Exception]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXLoginInfoDAOInsertOrUpdateSpec extends UserXLoginInfoDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException

  "An insert or update of an existing user join login info" should {
    "successfully return the saved user join login info" in new WithDepsApplication {
      init
      val insertedUserXLoginInfo: UserXLoginInfo = userXLoginInfo.copy(id = Some(1))
      Await.result(insert(userXLoginInfo), Duration.Inf) must
        beEqualTo(insertedUserXLoginInfo)
      val userXLoginInfo2 =
        insertedUserXLoginInfo.copy(loginInfoId = 2)
      Await.result(
        insertOrUpdate(userXLoginInfo2), Duration.Inf
      ) must beEqualTo(userXLoginInfo2)
    }
  }
  "An insert or update of a non-existing user join login info" should {
    "successfully return the saved user join login info" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(userXLoginInfo), Duration.Inf
      ) must beEqualTo(userXLoginInfo.copy(id = Some(1)))
    }
  }
  "An insert or update of a user join login info with an errorneous user id" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(userXLoginInfo.copy(userId = 4)),
        Duration.Inf
      ) must throwAn[SQLIntegrityConstraintViolationException]
    }
  }
  "An insert or update of a user join login info with an errorneous login info id" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(userXLoginInfo.copy(loginInfoId = 5)),
        Duration.Inf
      ) must throwAn[SQLIntegrityConstraintViolationException]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXLoginInfoDAOFindByIdSpec extends UserXLoginInfoDAOSpecHelper {
  "A search of an existing user join login info by its id" should {
    "successfully return the user join login info" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserXLoginInfo: UserXLoginInfo = userXLoginInfo.copy(id = Some(id))
      Await.result(insert(userXLoginInfo), Duration.Inf) must
        beEqualTo(insertedUserXLoginInfo)
      Await.result(findById(id), Duration.Inf) must
        beEqualTo(Some(insertedUserXLoginInfo))
    }
  }
  "A search of a non-existing user join login info by its id" should {
    "successfully return no user join login info" in new WithDepsApplication {
      init
      Await.result(findById(1), Duration.Inf) must beEqualTo(None)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXLoginInfoDAODeleteSpec extends UserXLoginInfoDAOSpecHelper {
  "The delete of an existing user join login info by its id" should {
    "return the number of user join login infos deleted (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserXLoginInfo: UserXLoginInfo = userXLoginInfo.copy(id = Some(id))
      Await.result(insert(userXLoginInfo), Duration.Inf) must
        beEqualTo(insertedUserXLoginInfo)
      Await.result(delete(id), Duration.Inf) must beEqualTo(1)
    }
  }
  "The delete of a non-existing user join login info by its id" should {
    "return 0 user join login infos deleted" in new WithDepsApplication {
      init
      Await.result(delete(1), Duration.Inf) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXLoginInfoDAOUpdateSpec extends UserXLoginInfoDAOSpecHelper {
  "The update of an existing user join login info" should {
    "return the number of user join login infos updated (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserXLoginInfo: UserXLoginInfo = userXLoginInfo.copy(id = Some(id))
      Await.result(insert(userXLoginInfo), Duration.Inf) must
        beEqualTo(insertedUserXLoginInfo)
      Await.result(
        update(insertedUserXLoginInfo.copy(loginInfoId = 2)),
        Duration.Inf
      ) must beEqualTo(1)
    }
  }
  "The update of a non-existing user join login info with an id" should {
    "return 0 user join login infos updated" in new WithDepsApplication {
      init
      Await.result(
        update(userXLoginInfo.copy(id = Some(1))), Duration.Inf
      ) must beEqualTo(0)
    }
  }
  "The update of a non-existing user join login info with no id" should {
    "return 0 user join login infos updated" in new WithDepsApplication {
      init
      Await.result(
        update(userXLoginInfo), Duration.Inf
      ) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXLoginInfoDAOGetUserReadableFromLoginInfoSpec extends UserXLoginInfoDAOSpecHelper {

  "The search of user join login info using a given login info" should {
    "return the user in readable format" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserXLoginInfo: UserXLoginInfo = userXLoginInfo.copy(id = Some(id))
      Await.result(insert(userXLoginInfo), Duration.Inf) must
        beEqualTo(insertedUserXLoginInfo)
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = loginInfo0.providerId,
        providerKey = loginInfo0.providerKey
      )
      val foundUser: UserReadable = UserReadable(
        id = Some(1),
        groupName = group.name,
        roleName = role.name,
        statusValue = status.value,
        userId = user.userId,
        avatarURL = user.avatarURL,
        email = user.email,
        firstName = user.firstName,
        lastName = user.lastName,
        username = user.username,
        usernameSuffix = user.usernameSuffix
      )
      Await.result(findById(1), Duration.Inf) must
        beEqualTo(Some(insertedUserXLoginInfo))
      Await.result(userXLoginInfoDAO.getUserReadableFromLoginInfo(
        loginInfo = inLoginInfo
      ), Duration.Inf) must beEqualTo(Some(foundUser))
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXLoginInfoDAOFindByLoginInfoSpec extends UserXLoginInfoDAOSpecHelper {

  "The search of user join login info using a given login info" should {
    "return the user join login info" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserXLoginInfo: UserXLoginInfo = userXLoginInfo.copy(id = Some(id))
      Await.result(insert(userXLoginInfo), Duration.Inf) must
        beEqualTo(insertedUserXLoginInfo)
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = loginInfo0.providerId,
        providerKey = loginInfo0.providerKey
      )
      val foundUser: User = user.copy(id = Some(1))
      Await.result(userXLoginInfoDAO.findByLoginInfo(
        loginInfo = inLoginInfo
      ), Duration.Inf) must beEqualTo(Some(foundUser))
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXLoginInfoDAOInsertIdentitySpec extends UserXLoginInfoDAOSpecHelper {

  "The insert of an identity" should {
    "return the added user login info" in new WithDepsApplication {
      init
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = "SS*^%*DHFHFGD",
        providerKey = "foo@gmail.com"
      )
      val inUser: UserReadable = UserReadable(
        id = None,
        roleName = "fooRole",
        statusValue = "fooStatus",
        userId = UUID.randomUUID().toString,
        groupName = "fooGroup2",
        avatarURL = None,
        email = "foo@gmail.com",
        firstName = Option("John"),
        lastName = Option("Doe"),
        username = "foo3",
        usernameSuffix = Option(2)
      )
      lazy val inIdentity: IdentityImpl =
        IdentityImpl(user = inUser, loginInfo = inLoginInfo)
      lazy val outUserXLoginInfo: UserXLoginInfo =
        UserXLoginInfo(id = Option(1), userId = 2, loginInfoId = 3)
      lazy val outUser: User = User(
        id = Some(2),
        groupId = 2,
        roleId = 1,
        statusId = 1,
        userId = inUser.userId,
        avatarURL = inUser.avatarURL,
        email = inUser.email,
        firstName = inUser.firstName,
        lastName = inUser.lastName,
        username = inUser.username,
        usernameSuffix = inUser.usernameSuffix
      )
      lazy val outGroup: UserGroup = UserGroup(
        id = Some(2),
        active = true,
        description = "A user group",
        name = inUser.groupName
      )
      lazy val outUserXGroup: UserXGroup = UserXGroup(
        id = Some(1),
        groupId = 2,
        userId = 2,
        userPermissionId = 1
      )
      Await.result(
        userXLoginInfoDAO.insertIdentity(inIdentity),
        Duration.Inf
      ) must beEqualTo((outUserXLoginInfo, outUser))
      Await.result(userFindById(1), Duration.Inf) must
        beEqualTo(Some(user))
      Await.result(userFindById(2), Duration.Inf) must
        beEqualTo(Some(outUser))
      Await.result(groupFindById(1), Duration.Inf) must
        beEqualTo(Some(group))
      Await.result(groupFindById(2), Duration.Inf) must
        beEqualTo(Some(outGroup))
      Await.result(userXGroupFindById(1), Duration.Inf) must
        beEqualTo(Some(outUserXGroup))
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXLoginInfoDAOParentSpec extends UserXLoginInfoDAOSpecHelper {
  override def is = s2"""
    These are the full User Join Login Info DAO specs
    ${"child0" ~ UserXLoginInfoDAOInsertSpec}
    ${"child1" ~ UserXLoginInfoDAOInsertOrUpdateSpec}
    ${"child2" ~ UserXLoginInfoDAOFindByIdSpec}
    ${"child3" ~ UserXLoginInfoDAODeleteSpec}
    ${"child4" ~ UserXLoginInfoDAOUpdateSpec}
    ${"child5" ~ UserXLoginInfoDAOGetUserReadableFromLoginInfoSpec}
    ${"child6" ~ UserXLoginInfoDAOFindByLoginInfoSpec}
    ${"child7" ~ UserXLoginInfoDAOInsertIdentitySpec}
  """
}
