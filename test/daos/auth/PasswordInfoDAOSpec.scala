package aurita.test.daos.auth

import java.util.UUID
import com.mohiva.play.silhouette
import org.junit.runner.RunWith
import org.specs2.execute._
import org.specs2.runner.JUnitRunner
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.Random
import aurita.daos.utility.DAOHelpersExceptions
import aurita.test.daos.utility.TestEnvironment
import aurita.models.auth.PasswordInfo

trait PasswordInfoDAOSpecHelper extends TestEnvironment {
  import aurita.daos.auth.PasswordInfoDAOInterface
  import scala.concurrent.ExecutionContext

  trait TestData extends PasswordInfoDAOInterface {
    import profile.api._
    import slick.basic.BasicAction
    import slick.dbio.{ NoStream, Effect }
    import aurita.models.CurrentStatus
    import aurita.models.auth.{ UserGroup, UserRole, LoginInfo }

    type MyBasicAction = BasicAction[PasswordInfo, NoStream, Effect.Write]

    protected val tableQuery = TableQuery[PasswordInfoTable]
    protected val infoTableQuery = TableQuery[LoginInfoTable]

    lazy val passwordInfo: PasswordInfo =
      PasswordInfo(
        id = None,
        loginInfoId= 1,
        hasher = "sadsaaf4422",
        password = "Fsdf80sdsd",
        salt = Some("dfs7678dsf")
      )

    lazy val loginInfo0: LoginInfo = LoginInfo(
      id = Some(1), providerId = "Sfsf9898", providerKey = "SDSFD2342424234"
    )

    lazy val loginInfo1: LoginInfo = LoginInfo(
      id = Some(2), providerId = "ghjhg898", providerKey = "DHuiiu5464"
    )

    def init = {
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
    }

    private def _insert(model: PasswordInfo): MyBasicAction =
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
    def findById(id: Long): Future[Option[PasswordInfo]] =
      db.run(tableQuery.filter(t => t.id === id).result.headOption)

    def insert(model: PasswordInfo): Future[PasswordInfo] = db.run(_insert(model))

    def update(model: PasswordInfo): Future[Int] = db.run(
      tableQuery.filter(t => t.id === model.id).update(model)
    )

    def insertOrUpdate(model: PasswordInfo): Future[PasswordInfo] = db.run(
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
object PasswordInfoDAOInsertSpec extends PasswordInfoDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException
  import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation

  "An insert of a unique password info" should {
    "successfully return the inserted passwordInfo" in new WithDepsApplication {
      init
      Await.result(insert(passwordInfo), Duration.Inf) must
        beEqualTo(passwordInfo.copy(id = Some(1)))
      val passwordInfo2: PasswordInfo =
        passwordInfo.copy(loginInfoId = 2, salt = Some("rewDD553reer"))
      Await.result(insert(passwordInfo2), Duration.Inf) must
        beEqualTo(passwordInfo2.copy(id = Some(2)))
    }
  }
  "An insert of a non-unique password info" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(insert(passwordInfo), Duration.Inf)
      Await.result(insert(passwordInfo), Duration.Inf) must
        throwAn[SQLIntegrityConstraintViolationException]
    }
  }
  "An insert of a password info with an errorneous hasher value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insert(passwordInfo.copy(hasher = null)), Duration.Inf
      ) must throwAn[Exception]
    }
  }
  "An insert of a password info with an errorneous login info id value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insert(passwordInfo.copy(loginInfoId = 4)), Duration.Inf
      ) must throwAn[Exception]
    }
  }
  "An insert of a password info with an errorneous password value" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insert(passwordInfo.copy(password = null)), Duration.Inf
      ) must throwAn[Exception]
    }
  }
  "An insert of a password info with a 254 char hasher value" should {
    "successfully return the inserted login info" in
      new WithDepsApplication {
      init
      val passwordInfo2: PasswordInfo = passwordInfo.copy(
        hasher = Random.nextString(254)
      )
      Await.result(insert(passwordInfo2), Duration.Inf) must
        beEqualTo(passwordInfo2.copy(id = Some(1)))
    }
  }
  "An insert of a password info with a 255 char hasher value" should {
    "return a failure" in new WithDepsApplication {
      init
      val passwordInfo2: PasswordInfo =
        passwordInfo.copy(hasher = Random.nextString(255))
      Await.result(insert(passwordInfo2), Duration.Inf) must
        throwAn[MysqlDataTruncation]
    }
  }
  "An insert of a password info with a 254 char password hash value" should {
    "successfully return the inserted login info" in
      new WithDepsApplication {
      init
      val passwordInfo2: PasswordInfo = passwordInfo.copy(
        password = Random.nextString(254)
      )
      Await.result(insert(passwordInfo2), Duration.Inf) must
        beEqualTo(passwordInfo2.copy(id = Some(1)))
    }
  }
  "An insert of a password info with a 255 char password hash value" should {
    "return a failure" in new WithDepsApplication {
      init
      val passwordInfo2: PasswordInfo =
        passwordInfo.copy(password = Random.nextString(255))
      Await.result(insert(passwordInfo2), Duration.Inf) must
        throwAn[MysqlDataTruncation]
    }
  }
  "An insert of a password info with a 254 char password salt value" should {
    "successfully return the inserted login info" in
      new WithDepsApplication {
      init
      val passwordInfo2: PasswordInfo = passwordInfo.copy(
        salt = Option(Random.nextString(254))
      )
      Await.result(insert(passwordInfo2), Duration.Inf) must
        beEqualTo(passwordInfo2.copy(id = Some(1)))
    }
  }
  "An insert of a password info with a 255 char password salt value" should {
    "return a failure" in new WithDepsApplication {
      init
      val passwordInfo2: PasswordInfo =
        passwordInfo.copy(salt = Option(Random.nextString(255)))
      Await.result(insert(passwordInfo2), Duration.Inf) must
        throwAn[MysqlDataTruncation]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAOInsertOrUpdateSpec extends PasswordInfoDAOSpecHelper {
  import java.sql.SQLIntegrityConstraintViolationException

  "An insert or update of an existing password info" should {
    "successfully return the saved password info" in new WithDepsApplication {
      init
      val insertedPasswordInfo: PasswordInfo = passwordInfo.copy(id = Some(1))
      Await.result(insert(passwordInfo), Duration.Inf) must
        beEqualTo(insertedPasswordInfo)
      val passwordInfo2 =
        insertedPasswordInfo.copy(salt = Option("Foo4"))
      Await.result(
        insertOrUpdate(passwordInfo2), Duration.Inf
      ) must beEqualTo(passwordInfo2)
    }
  }
  "An insert or update of a non-existing password info" should {
    "successfully return the saved password info" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(passwordInfo), Duration.Inf
      ) must beEqualTo(passwordInfo.copy(id = Some(1)))
    }
  }
  "An insert or update of a password info with an errorneous hasher" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(passwordInfo.copy(hasher = null)),
        Duration.Inf
      ) must throwAn[SQLIntegrityConstraintViolationException]
    }
  }
  "An insert or update of a password info with an errorneous login info id" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(passwordInfo.copy(loginInfoId = 5)),
        Duration.Inf
      ) must throwAn[SQLIntegrityConstraintViolationException]
    }
  }
  "An insert or update of a password info with an erroneous password hash" should {
    "return a failure" in new WithDepsApplication {
      init
      Await.result(
        insertOrUpdate(passwordInfo.copy(password = null)),
        Duration.Inf
      ) must throwAn[SQLIntegrityConstraintViolationException]
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAOFindByIdSpec extends PasswordInfoDAOSpecHelper {
  "A search of an existing password info by its id" should {
    "successfully return the password info" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedPasswordInfo: PasswordInfo = passwordInfo.copy(id = Some(id))
      Await.result(insert(passwordInfo), Duration.Inf) must
        beEqualTo(insertedPasswordInfo)
      Await.result(findById(id), Duration.Inf) must
        beEqualTo(Some(insertedPasswordInfo))
    }
  }
  "A search of a non-existing password info by its id" should {
    "successfully return no password info" in new WithDepsApplication {
      init
      Await.result(findById(1), Duration.Inf) must beEqualTo(None)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAODeleteSpec extends PasswordInfoDAOSpecHelper {
  "The delete of an existing password info by its id" should {
    "return the number of password infos deleted (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedPasswordInfo: PasswordInfo = passwordInfo.copy(id = Some(id))
      Await.result(insert(passwordInfo), Duration.Inf) must
        beEqualTo(insertedPasswordInfo)
      Await.result(delete(id), Duration.Inf) must beEqualTo(1)
    }
  }
  "The delete of a non-existing password info by its id" should {
    "return 0 password infos deleted" in new WithDepsApplication {
      init
      Await.result(delete(1), Duration.Inf) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAOUpdateSpec extends PasswordInfoDAOSpecHelper {
  "The update of an existing password info" should {
    "return the number of password infos updated (1)" in
      new WithDepsApplication {
      init
      val id: Long = 1
      val insertedPasswordInfo: PasswordInfo = passwordInfo.copy(id = Some(id))
      Await.result(insert(passwordInfo), Duration.Inf) must
        beEqualTo(insertedPasswordInfo)
      Await.result(
        update(insertedPasswordInfo.copy(loginInfoId = 2)),
        Duration.Inf
      ) must beEqualTo(1)
    }
  }
  "The update of a non-existing password info with an id" should {
    "return 0 password infos updated" in new WithDepsApplication {
      init
      Await.result(
        update(passwordInfo.copy(id = Some(1))), Duration.Inf
      ) must beEqualTo(0)
    }
  }
  "The update of a non-existing password info with no id" should {
    "return 0 password infos updated" in new WithDepsApplication {
      init
      Await.result(
        update(passwordInfo), Duration.Inf
      ) must beEqualTo(0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAOAddSpec extends PasswordInfoDAOSpecHelper {

  "The add of a new password info for a given login info" should {
    "return the added password info" in new WithDepsApplication {
      init
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = loginInfo0.providerId,
        providerKey = loginInfo0.providerKey
      )
      val inPasswordInfo: silhouette.api.util.PasswordInfo =
        silhouette.api.util.PasswordInfo(
          hasher = "dsdg7377D",
          password = "DD3423df",
          salt = Some("sdf23432")
        )
      val insertedPasswordInfo: PasswordInfo = PasswordInfo(
        id = Some(1),
        loginInfoId = 1,
        hasher = "dsdg7377D",
        password = "DD3423df",
        salt = Some("sdf23432")
      )
      Await.result(passwordInfoDAO.add(
        loginInfo = inLoginInfo,
        authInfo = inPasswordInfo
      ), Duration.Inf) must beEqualTo(inPasswordInfo)
      Await.result(findById(1), Duration.Inf) must beEqualTo(Some(insertedPasswordInfo))
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAOFindSpec extends PasswordInfoDAOSpecHelper {

  "The search of password info using a given login info" should {
    "return the password info" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedPasswordInfo: PasswordInfo = passwordInfo.copy(id = Some(id))
      Await.result(insert(passwordInfo), Duration.Inf) must
        beEqualTo(insertedPasswordInfo)
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = loginInfo0.providerId,
        providerKey = loginInfo0.providerKey
      )
      val outPasswordInfo: silhouette.api.util.PasswordInfo =
        silhouette.api.util.PasswordInfo(
          hasher = insertedPasswordInfo.hasher,
          password = insertedPasswordInfo.password,
          salt = insertedPasswordInfo.salt
        )
      Await.result(passwordInfoDAO.find(
        loginInfo = inLoginInfo
      ), Duration.Inf) must beEqualTo(Some(outPasswordInfo))
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAOUpdateAuthInfoSpec extends PasswordInfoDAOSpecHelper {

  "The update of an existing password info for a given login info" should {
    "return the updated password info" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedPasswordInfo: PasswordInfo = passwordInfo.copy(id = Some(id))
      Await.result(insert(passwordInfo), Duration.Inf) must
        beEqualTo(insertedPasswordInfo)
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = loginInfo0.providerId,
        providerKey = loginInfo0.providerKey
      )
      val inPasswordInfo: silhouette.api.util.PasswordInfo =
        silhouette.api.util.PasswordInfo(
          hasher = "dfggdf7377D",
          password = "JJ23df",
          salt = Some("55df23432")
        )
      val updatedPasswordInfo: PasswordInfo = PasswordInfo(
        id = Some(1),
        loginInfoId = 1,
        hasher = "dfggdf7377D",
        password = "JJ23df",
        salt = Some("55df23432")
      )
      Await.result(passwordInfoDAO.update(
        loginInfo = inLoginInfo,
        authInfo = inPasswordInfo
      ), Duration.Inf) must beEqualTo(inPasswordInfo)
      Await.result(findById(1), Duration.Inf) must beEqualTo(Some(updatedPasswordInfo))
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAOSaveSpec extends PasswordInfoDAOSpecHelper {

  "The save of an existing password info for a given login info" should {
    "return the updated password info" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedPasswordInfo: PasswordInfo = passwordInfo.copy(id = Some(id))
      Await.result(insert(passwordInfo), Duration.Inf) must
        beEqualTo(insertedPasswordInfo)
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = loginInfo0.providerId,
        providerKey = loginInfo0.providerKey
      )
      val inPasswordInfo: silhouette.api.util.PasswordInfo =
        silhouette.api.util.PasswordInfo(
          hasher = "dfggdf7377D",
          password = "JJ23df",
          salt = Some("55df23432")
        )
      val updatedPasswordInfo: PasswordInfo = PasswordInfo(
        id = Some(1),
        loginInfoId = 1,
        hasher = "dfggdf7377D",
        password = "JJ23df",
        salt = Some("55df23432")
      )
      Await.result(passwordInfoDAO.save(
        loginInfo = inLoginInfo,
        authInfo = inPasswordInfo
      ), Duration.Inf) must beEqualTo(inPasswordInfo)
      Await.result(findById(1), Duration.Inf) must beEqualTo(Some(updatedPasswordInfo))
    }
  }

  "The save of an non-existing password info for a given login info" should {
    "return the inserted password info" in new WithDepsApplication {
      init
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = loginInfo0.providerId,
        providerKey = loginInfo0.providerKey
      )
      val inPasswordInfo: silhouette.api.util.PasswordInfo =
        silhouette.api.util.PasswordInfo(
          hasher = "dfggdf7377D",
          password = "JJ23df",
          salt = Some("55df23432")
        )
      val updatedPasswordInfo: PasswordInfo = PasswordInfo(
        id = Some(1),
        loginInfoId = 1,
        hasher = "dfggdf7377D",
        password = "JJ23df",
        salt = Some("55df23432")
      )
      Await.result(passwordInfoDAO.save(
        loginInfo = inLoginInfo,
        authInfo = inPasswordInfo
      ), Duration.Inf) must beEqualTo(inPasswordInfo)
      Await.result(findById(1), Duration.Inf) must beEqualTo(Some(updatedPasswordInfo))
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAORemoveSpec extends PasswordInfoDAOSpecHelper {

  "The remove of an existing password associated with a login info" should {
    "delete the password info" in new WithDepsApplication {
      init
      val id: Long = 1
      val insertedPasswordInfo: PasswordInfo = passwordInfo.copy(id = Some(id))
      Await.result(insert(passwordInfo), Duration.Inf) must
        beEqualTo(insertedPasswordInfo)
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = loginInfo0.providerId,
        providerKey = loginInfo0.providerKey
      )
      Await.result(findById(1), Duration.Inf) must beEqualTo(Some(insertedPasswordInfo))
      Await.result(passwordInfoDAO.remove(inLoginInfo), Duration.Inf)
      Await.result(findById(1), Duration.Inf) must beEqualTo(None)
    }
  }
  "The remove of a non-existing password info assoicated with a login info" should {
    "do nothing" in new WithDepsApplication {
      init
      val inLoginInfo: silhouette.api.LoginInfo = silhouette.api.LoginInfo(
        providerID = loginInfo0.providerId,
        providerKey = loginInfo0.providerKey
      )
      Await.result(findById(1), Duration.Inf) must beEqualTo(None)      
      Await.result(passwordInfoDAO.remove(inLoginInfo), Duration.Inf)
      Await.result(findById(1), Duration.Inf) must beEqualTo(None)
    }
  }
}

@RunWith(classOf[JUnitRunner])
object PasswordInfoDAOParentSpec extends PasswordInfoDAOSpecHelper {
  override def is = s2"""
    These are the full Password Info DAO specs
    ${"child0" ~ PasswordInfoDAOInsertSpec}
    ${"child1" ~ PasswordInfoDAOInsertOrUpdateSpec}
    ${"child2" ~ PasswordInfoDAOFindByIdSpec}
    ${"child3" ~ PasswordInfoDAODeleteSpec}
    ${"child4" ~ PasswordInfoDAOUpdateSpec}
    ${"child6" ~ PasswordInfoDAOAddSpec}
    ${"child7" ~ PasswordInfoDAOFindSpec}
    ${"child8" ~ PasswordInfoDAOUpdateAuthInfoSpec}
    ${"child9" ~ PasswordInfoDAOSaveSpec}
    ${"child10" ~ PasswordInfoDAORemoveSpec}
  """
}
