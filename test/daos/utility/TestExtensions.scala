package aurita.test.daos.utility

import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import play.api.ApplicationLoader
import play.api.BuiltInComponents
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import play.api.db.slick.DbName
import play.api.db.slick.SlickComponents
import play.api.NoHttpFiltersComponents
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

class TestApplicationLoader extends ApplicationLoader {
  import play.api.ApplicationLoader.Context
  import play.api.BuiltInComponentsFromContext
  import play.api.LoggerConfigurator

  def load(context: Context) = {
    // make sure logging is configured
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }

    (
      new BuiltInComponentsFromContext(context) with TestAppComponents
    ).application
  }
}

trait TestAppComponents extends TestSlickComponents {
  import play.api.routing.{ Router, SimpleRouter }
  import _root_.router.Routes

  lazy val router: Router = new SimpleRouter {
    def routes: Router.Routes = Map.empty
  }
}

trait TestSlickComponents extends BuiltInComponents
  with SlickComponents
  with SlickEvolutionsComponents
  with NoHttpFiltersComponents {
  import play.api.db.evolutions.Evolutions
  import play.api.inject.{ SimpleInjector, Injector, NewInstanceInjector }

  // Define your dependencies and controllers
  lazy val dbConfig: DatabaseConfig[JdbcProfile] =
    slickApi.dbConfig[JdbcProfile](TestSlickComponents.dbName)

  override lazy val injector: Injector =
    new SimpleInjector(NewInstanceInjector) +
      router +
      cookieSigner +
      csrfTokenSigner +
      httpConfiguration +
      slickApi  // SlickApi
  lazy val dbNameVal: String = TestSlickComponents.dbName.value
  Evolutions.cleanupEvolutions(
    database = dbApi.database(dbNameVal),
    autocommit = true
  )
  Evolutions.applyEvolutions(
    database = dbApi.database(dbNameVal),
    autocommit = true
  )
}

object TestSlickComponents {
  lazy val dbName: DbName = DbName("test")
}

trait TestSlickApi {
  import play.api.Application
  import play.api.db.slick.SlickApi

  def app: Application

  lazy val api: SlickApi = app.injector.instanceOf[SlickApi]
  protected val dbConfig: DatabaseConfig[JdbcProfile] =
    api.dbConfig[JdbcProfile](TestSlickComponents.dbName)
}

trait TestEnvironment extends SpecificationLike
  with Mockito {
  import play.api.test.WithApplicationLoader
  import org.specs2.specification.Scope

  abstract class TestApplication
    extends WithApplicationLoader(new TestApplicationLoader)
    with TestSlickApi
    with Scope
}
