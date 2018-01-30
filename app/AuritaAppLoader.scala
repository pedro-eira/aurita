package aurita

import _root_.controllers.AssetsComponents
import com.softwaremill.macwire.wire
import play.api.{ ApplicationLoader, BuiltInComponentsFromContext }
import play.api.db.slick.SlickComponents
import play.api.db.slick.evolutions.SlickEvolutionsComponents
import play.api.i18n.I18nComponents
import play.filters.csrf.CSRFComponents
import play.filters.headers.SecurityHeadersComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import play.api.cache.ehcache.EhCacheComponents
import aurita.utility.auth.AuthenticationEnvironment

class AuritaAppLoader extends ApplicationLoader {
  import play.api.LoggerConfigurator
  import play.api.Application

  def load(context: Context): Application = {

    // make sure logging is configured
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }

    new AppComponents(context).application
  }
}

trait BackendActorSystemTag
trait MainActorSystemTag

class AppComponents(context: Context) extends AuritaSlickComponents(context)
  with AuthenticationEnvironment
  with EhCacheComponents
  with AssetsComponents
  with AhcWSComponents
  with HttpFiltersComponents
  with I18nComponents
  with CSRFComponents
  with SecurityHeadersComponents {
  import com.softwaremill.tagging._
  import com.typesafe.config.ConfigFactory
  import play.api.Application
  import play.api.routing.Router
  import play.api.cache.AsyncCacheApi
  import _root_.controllers.Assets
  import _root_.router.Routes
  import aurita.controllers.HomeController
  import aurita.actors.{ SocketClientFactory, SocketClientFactoryImpl }

  lazy val cacheApi: AsyncCacheApi = defaultCacheApi

  lazy val prefix: String = "/"
  lazy val router: Router = wire[Routes]

  implicit val myApplication: Application = application

  lazy val mainActorSystem = actorSystem.taggedWith[MainActorSystemTag]
  lazy val socketClientFactory: SocketClientFactory = wire[SocketClientFactoryImpl]
  lazy val homeController: HomeController = wire[HomeController]
}

abstract class AuritaSlickComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with SlickComponents
    with SlickEvolutionsComponents {
  import play.api.db.slick.DbName
  import slick.basic.DatabaseConfig
  import slick.jdbc.JdbcProfile

  lazy val dbName: DbName = DbName(
    environment.mode match {
      case play.api.Mode.Dev => "dev"
      case play.api.Mode.Test => "test"
      case play.api.Mode.Prod => "prod"
    }
  )

  val dbConfig: DatabaseConfig[JdbcProfile] =
    slickApi.dbConfig[JdbcProfile](dbName)

  import play.api.db.evolutions._

  if (environment.mode == play.api.Mode.Test) {
    Evolutions.cleanupEvolutions(database = dbApi.database(dbName.value))
  }

  Evolutions.applyEvolutions(database = dbApi.database(dbName.value))
}
