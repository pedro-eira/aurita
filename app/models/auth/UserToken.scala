package aurita.models.auth

import java.sql.Timestamp
import aurita.models.utility.Common

/** Provide a token for a [[User user]].
  *
  * Data model for the
  * [[aurita.daos.auth.UserTokenDAOInterface.UserTokenDAOImpl.tableQuery user token]]
  * database table.
  *
  * @constructor create a new user
  * @param id the primary key
  * @param userId the foreign key to the [[User user]] account
  * @param tokenId the token id
  * @param expiresOn the expiry time of the token
  */
case class UserToken(
  id: Option[Long], userId: Long, tokenId: String, expiresOn: Timestamp
) extends Common[UserToken] {
  import org.joda.time.DateTime

  def isExpired: Boolean = (new DateTime(expiresOn.getTime)).isBeforeNow
}
