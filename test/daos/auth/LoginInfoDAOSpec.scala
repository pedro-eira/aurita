package aurita.test.daos.auth

import java.util.UUID
import org.junit.runner.RunWith
import org.specs2.execute._
import org.specs2.runner.JUnitRunner
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.Random
import aurita.models.auth.LoginInfo
import aurita.daos.utility.DAOHelpersExceptions
import aurita.test.daos.utility.TestEnvironment

trait LoginInfoDAOSpecHelper extends TestEnvironment {
  import aurita.daos.auth.LoginInfoDAOInterface
  import scala.concurrent.ExecutionContext

  trait TestData extends LoginInfoDAOInterface {
    import profile.api._
    import slick.basic.BasicAction
    import slick.dbio.{ NoStream, Effect }

    type MyBasicAction = BasicAction[LoginInfo, NoStream, Effect.Write]

    protected val tableQuery = TableQuery[LoginInfoTable]

    lazy val loginInfo: LoginInfo =
      LoginInfo(
        id = None, providerId = "45ggww3AA", providerKey = "SS534gfe"
      )

    private def _insert(model: LoginInfo): MyBasicAction =
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
    def findById(id: Long): Future[Option[LoginInfo]] =
      db.run(tableQuery.filter(t => t.id === id).result.headOption)

    def insert(model: LoginInfo): Future[LoginInfo] = db.run(_insert(model))

    def update(model: LoginInfo): Future[Int] = db.run(
      tableQuery.filter(t => t.id === model.id).update(model)
    )

    def insertOrUpdate(model: LoginInfo): Future[LoginInfo] = db.run(
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
object LoginInfoDAOInsertSpec extends LoginInfoDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException
  import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation

  "An insert of a unique login info" should {
    "successfully return the inserted loginInfo" in new WithDepsApplication {
      init
      Await.result(insert(loginInfo), Duration.Inf) must
        beEqualTo(loginInfo.copy(id = Some(1)))
      val loginInfo2: LoginInfo = loginInfo.copy(providerId = "rewDD553reer")
      Await.result(insert(loginInfo2), Duration.Inf) must
        beEqualTo(loginInfo2.copy(id = Some(2)))
    }
  }
  "An insert of a non-unique login info" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(insert(loginInfo), Duration.Inf)
      Await.result(insert(loginInfo), Duration.Inf) must
        throwAn[SQLIntegrityConstraintViolationException]
    }
  }
  "An insert of a login info with an errorneous provider key value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insert(loginInfo.copy(providerKey = null)), Duration.Inf
      ) must throwAn[Exception]
    }
  }
  "An insert of a login info with an errorneous provider id value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insert(loginInfo.copy(providerId = null)), Duration.Inf
      ) must throwAn[Exception]
    }
  }
  "An insert of a loginInfo with a 254 char provider id value" should {
    "successfully return the inserted login info" in
      new WithDepsApplication {
      init
      val loginInfo2: LoginInfo = loginInfo.copy(
        providerId = Random.nextString(254)
      )
      Await.result(insert(loginInfo2), Duration.Inf) must
        beEqualTo(loginInfo2.copy(id = Some(1)))
    }
  }
  "An insert of a login info with a 255 char provider id value" should {
    "return a failure" in new WithDepsApplication {
      init
      val loginInfo2: LoginInfo =
        loginInfo.copy(providerId = Random.nextString(255))
      Await.result(insert(loginInfo2), Duration.Inf) must
        throwAn[MysqlDataTruncation]
    }
  }
  "An insert of a loginInfo with a 254 char provider key value" should {
    "successfully return the inserted login info" in
      new WithDepsApplication {
      init
      val loginInfo2: LoginInfo = loginInfo.copy(
        providerKey = Random.nextString(254)
      )
      Await.result(insert(loginInfo2), Duration.Inf) must
        beEqualTo(loginInfo2.copy(id = Some(1)))
    }
  }
  "An insert of a login info with a 255 char provider key value" should {
    "return a failure" in new WithDepsApplication {
      init
      val loginInfo2: LoginInfo =
        loginInfo.copy(providerKey = Random.nextString(255))
      Await.result(insert(loginInfo2), Duration.Inf) must
        throwAn[MysqlDataTruncation]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object LoginInfoDAOInsertOrUpdateSpec extends LoginInfoDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException

  "An insert or update of an existing login info" should {
    "successfully return the saved login info" in new WithDepsApplication {
      init
      val insertedLoginInfo: LoginInfo = loginInfo.copy(id = Some(1))
      Await.result(insert(loginInfo), Duration.Inf) must
        beEqualTo(insertedLoginInfo)
      val loginInfo2 =
        insertedLoginInfo.copy(providerKey = "AnotherTest.")
      Await.result(
        insertOrUpdate(loginInfo2), Duration.Inf
      ) must beEqualTo(loginInfo2)
    }
  }
  "An insert or update of a non-existing login info" should {
    "successfully return the saved login info" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(loginInfo), Duration.Inf
      ) must beEqualTo(loginInfo.copy(id = Some(1)))
    }
  }
  "An insert or update of a login info with an errorneous provider key" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(loginInfo.copy(providerKey = null)),
        Duration.Inf
      ) must throwAn[SQLIntegrityConstraintViolationException]
    }
  }
  "An insert or update of a login info with an errorneous provider id" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(loginInfo.copy(providerId = null)),
        Duration.Inf
      ) must throwAn[SQLIntegrityConstraintViolationException]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object LoginInfoDAOFindByIdSpec extends LoginInfoDAOSpecHelper {
  "A search of an existing login info by its id" should {
    "successfully return the login info" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedLoginInfo: LoginInfo = loginInfo.copy(id = Some(id))
      Await.result(insert(loginInfo), Duration.Inf) must
        beEqualTo(insertedLoginInfo)
      Await.result(findById(id), Duration.Inf) must
        beEqualTo(Some(insertedLoginInfo))
    }
  }
  "A search of a non-existing login info by its id" should {
    "successfully return no login info" in new WithDepsApplication {
      init
      Await.result(findById(1), Duration.Inf) must beEqualTo(None)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object LoginInfoDAODeleteSpec extends LoginInfoDAOSpecHelper {
  "The delete of an existing login info by its id" should {
    "return the number of login infos deleted (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedLoginInfo: LoginInfo = loginInfo.copy(id = Some(id))
      Await.result(insert(loginInfo), Duration.Inf) must
        beEqualTo(insertedLoginInfo)
      Await.result(delete(id), Duration.Inf) must beEqualTo(1)
    }
  }
  "The delete of a non-existing login info by its id" should {
    "return 0 login infos deleted" in new WithDepsApplication {
      init
      Await.result(delete(1), Duration.Inf) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object LoginInfoDAOUpdateSpec extends LoginInfoDAOSpecHelper {
  "The update of an existing login info" should {
    "return the number of login infos updated (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedLoginInfo: LoginInfo = loginInfo.copy(id = Some(id))
      Await.result(insert(loginInfo), Duration.Inf) must
        beEqualTo(insertedLoginInfo)
      Await.result(
        update(insertedLoginInfo.copy(providerKey = "foo2")),
        Duration.Inf
      ) must beEqualTo(1)
    }
  }
  "The update of a non-existing login info with an id" should {
    "return 0 login infos updated" in new WithDepsApplication {
      init
      Await.result(
        update(loginInfo.copy(id = Some(1))), Duration.Inf
      ) must beEqualTo(0)
    }
  }
  "The update of a non-existing login info with no id" should {
    "return 0 login infos updated" in new WithDepsApplication {
      init
      Await.result(
        update(loginInfo), Duration.Inf
      ) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object LoginInfoDAOParentSpec extends LoginInfoDAOSpecHelper {
  override def is = s2"""
    These are the full Login Info DAO specs
    ${"child0" ~ LoginInfoDAOInsertSpec}
    ${"child1" ~ LoginInfoDAOInsertOrUpdateSpec}
    ${"child2" ~ LoginInfoDAOFindByIdSpec}
    ${"child3" ~ LoginInfoDAODeleteSpec}
    ${"child4" ~ LoginInfoDAOUpdateSpec}
  """
}
