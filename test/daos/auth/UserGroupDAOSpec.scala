package aurita.test.daos.auth

import java.util.UUID
import org.junit.runner.RunWith
import org.specs2.execute._
import org.specs2.runner.JUnitRunner
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import scala.util.Random
import aurita.models.auth.UserGroup
import aurita.daos.utility.DAOHelpersExceptions
import aurita.test.daos.utility.TestEnvironment

trait UserGroupDAOSpecHelper extends TestEnvironment {
  import aurita.daos.auth.UserGroupDAOInterface
  import scala.concurrent.ExecutionContext

  trait TestData extends UserGroupDAOInterface {
    import profile.api._
    import slick.basic.BasicAction
    import slick.dbio.{ NoStream, Effect }

    type MyBasicAction = BasicAction[UserGroup, NoStream, Effect.Write]

    protected val tableQuery = TableQuery[UserGroupTable]

    lazy val userGroup: UserGroup =
      UserGroup(
        id = None,
        active = true,
        description = "A test group",
        name = "foo"
      )

    private def _insert(model: UserGroup): MyBasicAction =
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
    def findById(id: Long): Future[Option[UserGroup]] =
      db.run(tableQuery.filter(t => t.id === id).result.headOption)

    def insert(model: UserGroup): Future[UserGroup] = db.run(_insert(model))

    def update(model: UserGroup): Future[Int] = db.run(
      tableQuery.filter(t => t.id === model.id).update(model)
    )

    def insertOrUpdate(model: UserGroup): Future[UserGroup] = db.run(
      (tableQuery returning tableQuery.map(_.id)).insertOrUpdate(model)
    ) map { _ match { 
      case None => model
      case Some(id) => model.copy(id = Option(id))
    } }

    def init() = {}
  }

  implicit val ec: ExecutionContext = ExecutionContext.global

  class WithDepsApplication()(
    implicit val ec: ExecutionContext
  ) extends TestApplication with TestData
}

@RunWith(classOf[JUnitRunner])
object UserGroupDAOInsertSpec extends UserGroupDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException
  import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation

  "An insert of a unique user group" should {
    "successfully return the inserted userGroup" in new WithDepsApplication {
      init
      Await.result(insert(userGroup), Duration.Inf) must
        beEqualTo(userGroup.copy(id = Some(1)))
      val userGroup2: UserGroup = userGroup.copy(name = "foo2")
      Await.result(insert(userGroup2), Duration.Inf) must
        beEqualTo(userGroup2.copy(id = Some(2)))
    }
  }
  "An insert of a non-unique user group" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(insert(userGroup), Duration.Inf)
      Await.result(insert(userGroup), Duration.Inf) must
        throwAn[SQLIntegrityConstraintViolationException]
    }
  }
  "An insert of a user group with an errorneous value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insert(userGroup.copy(name = null)), Duration.Inf
      ) must throwAn[Exception]
    }
  }
  "An insert of a userGroup with a 254 char description value" should {
    "successfully return the inserted user group" in
      new WithDepsApplication {
      init
      val userGroup2: UserGroup = userGroup.copy(
        description = Random.nextString(128)
      )
      Await.result(insert(userGroup2), Duration.Inf) must
        beEqualTo(userGroup2.copy(id = Some(1)))
    }
  }
  "An insert of a user group with a 255 char description value" should {
    "return a failure" in new WithDepsApplication {
      init
      val userGroup2: UserGroup =
        userGroup.copy(description = Random.nextString(255))
      Await.result(insert(userGroup2), Duration.Inf) must
        throwAn[MysqlDataTruncation]
    }
  }
  "An insert of a userGroup with a 127 char name value" should {
    "successfully return the inserted user group" in
      new WithDepsApplication {
      init
      val userGroup2: UserGroup =
        userGroup.copy(name = Random.nextString(127))
      Await.result(insert(userGroup2), Duration.Inf) must
        beEqualTo(userGroup2.copy(id = Some(1)))
    }
  }
  "An insert of a user group with a 128 char name value" should {
    "return a failure" in new WithDepsApplication {
      init
      val userGroup2: UserGroup = userGroup.copy(name = Random.nextString(128))
      Await.result(insert(userGroup2), Duration.Inf) must
        throwAn[MysqlDataTruncation]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserGroupDAOInsertOrUpdateSpec extends UserGroupDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException

  "An insert or update of an existing user group" should {
    "successfully return the saved user group" in new WithDepsApplication {
      init
      val insertedUserGroup: UserGroup = userGroup.copy(id = Some(1))
      Await.result(insert(userGroup), Duration.Inf) must
        beEqualTo(insertedUserGroup)
      val userGroup2 =
        insertedUserGroup.copy(description = "Another test.")
      Await.result(
        insertOrUpdate(userGroup2), Duration.Inf
      ) must beEqualTo(userGroup2)
    }
  }
  "An insert or update of a non-existing user group" should {
    "successfully return the saved user group" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(userGroup), Duration.Inf
      ) must beEqualTo(userGroup.copy(id = Some(1)))
    }
  }
  "An insert or update of a user group with an errorneous value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(userGroup.copy(description = null)),
        Duration.Inf
      ) must throwAn[SQLIntegrityConstraintViolationException]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserGroupDAOFindByIdSpec extends UserGroupDAOSpecHelper {
  "A search of an existing user group by its id" should {
    "successfully return the user group" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserGroup: UserGroup = userGroup.copy(id = Some(id))
      Await.result(insert(userGroup), Duration.Inf) must
        beEqualTo(insertedUserGroup)
      Await.result(findById(id), Duration.Inf) must
        beEqualTo(Some(insertedUserGroup))
    }
  }
  "A search of a non-existing user group by its id" should {
    "successfully return no user group" in new WithDepsApplication {
      init
      Await.result(findById(1), Duration.Inf) must beEqualTo(None)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserGroupDAODeleteSpec extends UserGroupDAOSpecHelper {
  "The delete of an existing user group by its id" should {
    "return the number of user groups deleted (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserGroup: UserGroup = userGroup.copy(id = Some(id))
      Await.result(insert(userGroup), Duration.Inf) must
        beEqualTo(insertedUserGroup)
      Await.result(delete(id), Duration.Inf) must beEqualTo(1)
    }
  }
  "The delete of a non-existing user group by its id" should {
    "return 0 user groups deleted" in new WithDepsApplication {
      init
      Await.result(delete(1), Duration.Inf) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserGroupDAOUpdateSpec extends UserGroupDAOSpecHelper {
  "The update of an existing user group" should {
    "return the number of user groups updated (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedUserGroup: UserGroup = userGroup.copy(id = Some(id))
      Await.result(insert(userGroup), Duration.Inf) must
        beEqualTo(insertedUserGroup)
      Await.result(
        update(insertedUserGroup.copy(name = "foo2")),
        Duration.Inf
      ) must beEqualTo(1)
    }
  }
  "The update of a non-existing user group with an id" should {
    "return 0 user groups updated" in new WithDepsApplication {
      init
      Await.result(
        update(userGroup.copy(id = Some(1))), Duration.Inf
      ) must beEqualTo(0)
    }
  }

  "The update of a non-existing user group with no id" should {
    "return 0 user groups updated" in new WithDepsApplication {
      init
      Await.result(
        update(userGroup), Duration.Inf
      ) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object UserGroupDAOParentSpec extends UserGroupDAOSpecHelper {
  override def is = s2"""
    These are the full User Group DAO specs
    ${"child0" ~ UserGroupDAOInsertSpec}
    ${"child1" ~ UserGroupDAOInsertOrUpdateSpec}
    ${"child2" ~ UserGroupDAOFindByIdSpec}
    ${"child3" ~ UserGroupDAODeleteSpec}
    ${"child4" ~ UserGroupDAOUpdateSpec}
  """
}
