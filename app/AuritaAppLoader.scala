package aurita

import _root_.controllers.AssetsComponents
import com.softwaremill.macwire.wire
import play.api.{ ApplicationLoader, BuiltInComponentsFromContext }
import play.api.i18n.I18nComponents
import play.filters.csrf.CSRFComponents
import play.filters.headers.SecurityHeadersComponents
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents

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

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with AssetsComponents
  with HttpFiltersComponents
  with I18nComponents
  with CSRFComponents
  with SecurityHeadersComponents {
  import com.typesafe.config.ConfigFactory
  import play.api.Application
  import play.api.routing.Router
  import _root_.controllers.Assets
  import _root_.router.Routes
  import aurita.controllers.HomeController

  lazy val prefix: String = "/"
  lazy val router: Router = wire[Routes]

  implicit val myApplication: Application = application

  lazy val homeController: HomeController = wire[HomeController]
}
