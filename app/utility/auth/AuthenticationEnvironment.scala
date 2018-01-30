package aurita.utility.auth

import com.mohiva.play.silhouette.api.{ Silhouette, SilhouetteProvider }
import play.api.cache.AsyncCacheApi
import play.api.libs.ws.WSClient
import play.api.Configuration
import scala.concurrent.ExecutionContext
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import aurita.daos.auth.{
  OAuth1InfoDAOInterface,
  OAuth2InfoDAOInterface,
  PasswordInfoDAOInterface
}

trait AuthenticationEnvironment
  extends PasswordInfoDAOInterface
  with OAuth1InfoDAOInterface
  with OAuth2InfoDAOInterface {
  import com.softwaremill.tagging._
  import com.softwaremill.macwire.wire
  import com.mohiva.play.silhouette.api.util.{
    CacheLayer, HTTPLayer, IDGenerator, PlayHTTPLayer
  }
  import com.mohiva.play.silhouette.impl.util.{
    DefaultFingerprintGenerator, PlayCacheLayer, SecureRandomIDGenerator
  }
  import com.mohiva.play.silhouette.api.{
    Environment, EventBus, RequestProvider
  }
  import com.mohiva.play.silhouette.impl.providers.{
    CredentialsProvider, SocialProviderRegistry
  }
  import com.mohiva.play.silhouette.impl.providers.{
    DefaultSocialStateHandler, SocialStateHandler
  }
  import com.mohiva.play.silhouette.crypto.{
    JcaCrypter,
    JcaCrypterSettings,
    JcaSigner,
    JcaSignerSettings
  }
  import com.mohiva.play.silhouette.api.crypto.Signer
  import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
  import com.mohiva.play.silhouette.api.crypto.{
    Crypter, CrypterAuthenticatorEncoder
  }
  import com.mohiva.play.silhouette.password.BCryptPasswordHasher
  import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
  import com.mohiva.play.silhouette.api.services.AuthenticatorService
  import com.mohiva.play.silhouette.impl.authenticators.{
    JWTAuthenticatorSettings, JWTAuthenticatorService
  }
  import net.ceedubs.ficus.Ficus._
  import com.mohiva.play.silhouette.api.util.{
    Clock,
    FingerprintGenerator,
    PasswordHasher,
    PasswordHasherRegistry,
    PasswordInfo
  }
  import com.mohiva.play.silhouette.password.BCryptPasswordHasher  
  import net.ceedubs.ficus.Ficus._
  import net.ceedubs.ficus.readers.EnumerationReader._
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._
  import aurita.daos.auth.{
    UserService, UserServiceImpl, UserServiceDAO, UserServiceDAOImpl
  }
  import com.mohiva.play.silhouette.api.actions.{
    SecuredAction,
    SecuredErrorHandler,
    SecuredRequestHandler,
    UnsecuredAction,
    UnsecuredErrorHandler,
    UnsecuredRequestHandler,
    UserAwareAction,
    UserAwareRequestHandler,    
    DefaultSecuredAction,
    DefaultSecuredRequestHandler,
    DefaultUnsecuredAction,
    DefaultUnsecuredErrorHandler,
    DefaultUnsecuredRequestHandler,
    DefaultUserAwareAction,
    DefaultUserAwareRequestHandler
  }
  import play.api.i18n.MessagesApi
  import com.mohiva.play.silhouette.api.services.AvatarService
  import com.mohiva.play.silhouette.impl.services.GravatarService
  import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
  import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
  import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
  import aurita.utility.auth.CustomSecuredErrorHandler
  import scala.concurrent.ExecutionContext
  import play.api.mvc.BodyParsers
  import com.mohiva.play.silhouette.impl.providers.oauth1.TwitterProvider
  import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{
    CookieSecretProvider, CookieSecretSettings
  }
  import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
  import com.mohiva.play.silhouette.impl.providers.{
    OAuth1Info, OAuth1Settings, OAuth2Info, OAuth2Settings
  }
  import com.mohiva.play.silhouette.impl.providers.oauth2.{
    FacebookProvider, GoogleProvider, LinkedInProvider
  }

  trait AuthenticatorCrypter
  trait OAuth1TokenSecretCrypter
  trait AuthenticatorEncoder

  implicit val materializer: akka.stream.Materializer

  val silhouetteDefaultBodyParser: BodyParsers.Default = new BodyParsers.Default
  implicit val executionContext: ExecutionContext
  val defaultCacheApi: AsyncCacheApi
  val wsClient: WSClient
  def configuration: Configuration
  val dbConfig: DatabaseConfig[JdbcProfile]
  val messagesApi: MessagesApi

  /**
   * Configures Silhouette.
   */
  lazy val cacheLayer: CacheLayer = wire[PlayCacheLayer]
  lazy val httpLayer: HTTPLayer = wire[PlayHTTPLayer]
  lazy val eventBus: EventBus = wire[EventBus]
  lazy val fingerprintGenerator: FingerprintGenerator =
    new DefaultFingerprintGenerator(includeRemoteAddress = false)
  lazy val clock: Clock = wire[Clock]
  lazy val jcaCrypterSettings: JcaCrypterSettings =
    configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")
  lazy val jcaCrypter: Crypter @@ AuthenticatorCrypter =
    wire[JcaCrypter].taggedWith[AuthenticatorCrypter]
  lazy val authenticatorEncoder: CrypterAuthenticatorEncoder @@ AuthenticatorEncoder =
    wire[_CrypterAuthenticatorEncoder].taggedWith[AuthenticatorEncoder]
  lazy val settings: JWTAuthenticatorSettings =
    configuration.underlying.as[JWTAuthenticatorSettings]("silhouette.authenticator")
  lazy val idGenerator: IDGenerator = new SecureRandomIDGenerator()
  lazy val authenticatorService: AuthenticatorService[JWTAuthenticator] =
    wire[_AuthenticatorService]
  lazy val requestProviders: Seq[RequestProvider] = Seq()
  lazy val userServiceDAO: UserServiceDAO = wire[UserServiceDAOImpl]
  lazy val userService: UserService = wire[UserServiceImpl]
  lazy val env: Environment[DefaultEnv] = Environment[DefaultEnv](
    userService, authenticatorService, requestProviders, eventBus
  )

  lazy val oAuth1TokenSecretSigner: Signer = {
    val config = configuration.underlying.as[JcaSignerSettings](
      "silhouette.oauth1TokenSecretProvider.signer"
    )
    new JcaSigner(config)
  }

  lazy val oAuth1TokenSecretCrypter: Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings](
      "silhouette.oauth1TokenSecretProvider.crypter"
    )
    new JcaCrypter(config)
  }

  lazy val oAuth1TokenSecretProvider = new CookieSecretProvider(
    configuration.underlying.as[CookieSecretSettings]("silhouette.oauth1TokenSecretProvider"),
    oAuth1TokenSecretSigner,
    oAuth1TokenSecretCrypter,
    clock
  )

  lazy val oAuth2TokenSecretSigner: Signer = {
    val config = configuration.underlying.as[JcaSignerSettings](
      "silhouette.oauth2TokenSecretProvider.signer"
    )
    new JcaSigner(config)
  }

  lazy val oAuth2StateHandler: SocialStateHandler = new DefaultSocialStateHandler(
    handlers = Set(), signer = oAuth2TokenSecretSigner
  )
    
  lazy val facebookProvider = new FacebookProvider(
    httpLayer = httpLayer,
    stateHandler = oAuth2StateHandler,
    settings = configuration.underlying.as[OAuth2Settings]("silhouette.facebook")
  )

  lazy val googleProvider = new GoogleProvider(
    httpLayer = httpLayer,
    stateHandler = oAuth2StateHandler,
    settings = configuration.underlying.as[OAuth2Settings]("silhouette.google")
  )

  lazy val linkedinProvider = new LinkedInProvider(
    httpLayer = httpLayer,
    stateHandler = oAuth2StateHandler,
    settings = configuration.underlying.as[OAuth2Settings]("silhouette.linkedin")
  )

  lazy val twitterProvider = {
    val settings = configuration.underlying.as[OAuth1Settings]("silhouette.twitter")

    new TwitterProvider(
      httpLayer = httpLayer,
      service = new PlayOAuth1Service(settings),
      tokenSecretProvider = oAuth1TokenSecretProvider,
      settings = settings
    )
  }

  lazy val socialProviderRegistry = SocialProviderRegistry(
    Seq(facebookProvider, googleProvider, linkedinProvider, twitterProvider)
  )

  lazy val securedErrorHandler: SecuredErrorHandler =
    wire[CustomSecuredErrorHandler]
  lazy val securedRequestHandler: SecuredRequestHandler =
    wire[DefaultSecuredRequestHandler]
  lazy val securedAction: SecuredAction = wire[DefaultSecuredAction]
  lazy val unsecuredErrorHandler: UnsecuredErrorHandler =
    wire[DefaultUnsecuredErrorHandler]
  lazy val unsecuredRequestHandler: UnsecuredRequestHandler =
    wire[DefaultUnsecuredRequestHandler]
  lazy val unsecuredAction: UnsecuredAction = wire[DefaultUnsecuredAction]
  lazy val userAwareRequestHandler: UserAwareRequestHandler =
    wire[DefaultUserAwareRequestHandler]
  lazy val userAwareAction: UserAwareAction = wire[DefaultUserAwareAction]
  lazy val avatarService: AvatarService = new GravatarService(httpLayer = httpLayer)
  lazy val passwordHasher: PasswordHasher = new BCryptPasswordHasher
  lazy val passwordHasherRegistry: PasswordHasherRegistry =
    new PasswordHasherRegistry(current = passwordHasher)
  lazy val passwordInfoDelegableDAO: DelegableAuthInfoDAO[PasswordInfo] =
    new PasswordInfoDAOImpl
  lazy val oAuth1InfoDelegableDAO: DelegableAuthInfoDAO[OAuth1Info] = oAuth1InfoDAO
  lazy val oAuth2InfoDelegableDAO: DelegableAuthInfoDAO[OAuth2Info] = oAuth2InfoDAO
  lazy val authInfoRepository: AuthInfoRepository = new DelegableAuthInfoRepository(
    passwordInfoDelegableDAO, oAuth1InfoDelegableDAO, oAuth2InfoDelegableDAO
  )
  lazy val credentialsProvider: CredentialsProvider = wire[CredentialsProvider]

  lazy val silhouette: Silhouette[DefaultEnv] = wire[SilhouetteProvider[DefaultEnv]]

  protected case class _CrypterAuthenticatorEncoder(
    crypter: Crypter @@ AuthenticatorCrypter
  ) extends CrypterAuthenticatorEncoder(crypter)

  protected case class _AuthenticatorService(
    settings: JWTAuthenticatorSettings,
    encoder: CrypterAuthenticatorEncoder @@ AuthenticatorEncoder,
    idGenerator: IDGenerator,
    clock: Clock
  ) extends JWTAuthenticatorService(
    settings = settings,
    repository = None,
    authenticatorEncoder = encoder,
    idGenerator = idGenerator,
    clock = clock
  )
}
