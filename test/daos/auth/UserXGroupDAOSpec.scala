package aurita.test.daos.auth

import java.util.UUID
import org.junit.runner.RunWith
import org.specs2.execute._
import org.specs2.runner.JUnitRunner
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.Random
import aurita.models.auth.UserXGroup
import aurita.daos.utility.DAOHelpersExceptions
import aurita.test.daos.utility.TestEnvironment
import aurita.models.auth.{ User, UserReadable }

trait UserXGroupDAOSpecHelper extends TestEnvironment {
  import aurita.daos.auth.UserXGroupDAOInterface
  import scala.concurrent.ExecutionContext

  trait TestData extends UserXGroupDAOInterface {
    import profile.api._
    import slick.basic.BasicAction
    import slick.dbio.{ NoStream, Effect }
    import aurita.models.CurrentStatus
    import aurita.models.auth.{ User, UserGroup, UserPermission, UserRole }

    type MyBasicAction = BasicAction[UserXGroup, NoStream, Effect.Write]

    protected val tableQuery = TableQuery[UserXGroupTable]
    protected val roleTableQuery = TableQuery[UserRoleTable]
    protected val groupTableQuery = TableQuery[UserGroupTable]
    protected val permissionTableQuery = TableQuery[UserPermissionTable]
    protected val userTableQuery = TableQuery[UserTable]
    protected val statusTableQuery = TableQuery[CurrentStatusTable]

    lazy val userXGroup: UserXGroup =
      UserXGroup(id = None, userId = 1, groupId = 1, userPermissionId = 1)

    lazy val userXGroup2: UserXGroup =
      UserXGroup(id = None, userId = 1, groupId = 2, userPermissionId = 1)

    lazy val userPermission: UserPermission = UserPermission(
      id = Some(1),
      roleId = 1,
      active = true,
      read = true,
      write = true,
      execute = true
    )

    lazy val group0: UserGroup = UserGroup(
      id = Some(1),
      active = true,
      description = "NA",
      name = "foo"
    )

    lazy val group1: UserGroup = UserGroup(
      id = Some(2),
      active = true,
      description = "NA",
      name = "foo2"
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
            user
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

    private def _insert(model: UserXGroup): MyBasicAction =
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
    def findById(id: Long): Future[Option[UserXGroup]] =
      db.run(tableQuery.filter(t => t.id === id).result.headOption)

    def insert(model: UserXGroup): Future[UserXGroup] = db.run(_insert(model))

    def update(model: UserXGroup): Future[Int] = db.run(
      tableQuery.filter(t => t.id === model.id).update(model)
    )

    def insertOrUpdate(model: UserXGroup): Future[UserXGroup] = db.run(
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
object UserXGroupDAOInsertSpec extends UserXGroupDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException
  import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation

  "An insert of a unique user group" should {
    "successfully return the inserted userXGroup" in new WithDepsApplication {
      init
      Await.result(insert(userXGroup), Duration.Inf) must
        beEqualTo(userXGroup.copy(id = Some(1)))
      Await.result(insert(userXGroup2), Duration.Inf) must
        beEqualTo(userXGroup2.copy(id = Some(2)))
    }
  }
  
  "An insert of a non-unique user group" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(insert(userXGroup), Duration.Inf)
      Await.result(insert(userXGroup), Duration.Inf) must
        throwAn[SQLIntegrityConstraintViolationException]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXGroupDAOInsertOrUpdateSpec extends UserXGroupDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException

  "An insert or update of an existing user join group" should {
    "successfully return the saved user join group" in new WithDepsApplication {
      init
      val insertedUserXGroup: UserXGroup = userXGroup.copy(id = Some(1))
      Await.result(insert(userXGroup), Duration.Inf) must
        beEqualTo(insertedUserXGroup)
      val userXGroup3 =
        insertedUserXGroup.copy(groupId = 2)
      Await.result(
        insertOrUpdate(userXGroup3), Duration.Inf
      ) must beEqualTo(userXGroup3)
    }
  }

  "An insert or update of a non-existing user join group" should {
    "successfully return the saved user join group" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(userXGroup), Duration.Inf
      ) must beEqualTo(userXGroup.copy(id = Some(1)))
    }
  }

  "An insert or update of a user join group with an errorneous value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(userXGroup.copy(groupId = 3)),
        Duration.Inf
      ) must throwAn[
          SQLIntegrityConstraintViolationException
      ]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXGroupDAOFindByIdSpec extends UserXGroupDAOSpecHelper {
  "A search of an existing user join group by its id" should {
    "successfully return the user join group" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserXGroup: UserXGroup = userXGroup.copy(id = Some(id))
      Await.result(insert(userXGroup), Duration.Inf) must
        beEqualTo(insertedUserXGroup)
      Await.result(findById(id), Duration.Inf) must
        beEqualTo(Some(insertedUserXGroup))
    }
  }
  "A search of a non-existing user join group by its id" should {
    "successfully return no user join group" in new WithDepsApplication {
      init
      Await.result(findById(1), Duration.Inf) must
        beEqualTo(None)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXGroupDAODeleteSpec extends UserXGroupDAOSpecHelper {
  "The delete of an existing user group by its id" should {
    "return the number of user groups deleted (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserXGroup: UserXGroup = userXGroup.copy(id = Some(id))
      Await.result(insert(userXGroup), Duration.Inf) must
        beEqualTo(insertedUserXGroup)
      Await.result(delete(id), Duration.Inf) must
        beEqualTo(1)
    }
  }
  "The delete of a non-existing user group by its id" should {
    "return 0 user groups deleted" in new WithDepsApplication {
      init
      Await.result(delete(1), Duration.Inf) must
        beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXGroupDAOUpdateSpec extends UserXGroupDAOSpecHelper {
  "The update of an existing user join group" should {
    "return the number of user join groups updated (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserXGroup: UserXGroup = userXGroup.copy(id = Some(id))
      Await.result(insert(userXGroup), Duration.Inf) must
        beEqualTo(insertedUserXGroup)
      Await.result(
        update(insertedUserXGroup.copy(groupId = 2)),
        Duration.Inf
      ) must beEqualTo(1)
    }
  }
  "The update of a non-existing user join group with an id" should {
    "return 0 user join groups updated" in new WithDepsApplication {
      init
      Await.result(
        update(userXGroup.copy(id = Some(1))), Duration.Inf
      ) must beEqualTo(0)
    }
  }
  "The update of a non-existing user join group with no id" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        update(userXGroup), Duration.Inf
      ) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserXGroupDAOParentSpec extends UserXGroupDAOSpecHelper {
  override def is = s2"""
    These are the full User Join Group DAO specs
    ${"child0" ~ UserXGroupDAOInsertSpec}
    ${"child1" ~ UserXGroupDAOInsertOrUpdateSpec}
    ${"child2" ~ UserXGroupDAOFindByIdSpec}
    ${"child3" ~ UserXGroupDAODeleteSpec}
    ${"child4" ~ UserXGroupDAOUpdateSpec}
  """
}
