package aurita.utility.auth

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import aurita.models.auth.IdentityImpl

/**
 * The default env.
 */
trait DefaultEnv extends Env {
  type I = IdentityImpl
  type A = JWTAuthenticator
}
