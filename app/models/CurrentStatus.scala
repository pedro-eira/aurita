package aurita.models

import aurita.models.utility.Common

/** Provide the availability status of some entity.
  *
  * Data model for the
  * [[aurita.daos.CurrentStatusDAOInterface.CurrentStatusDAOImpl.tableQuery status]]
  * database table.
  *
  * @constructor create a new status for an entity.
  * @param id the primary key
  * @param active whether the entity is available
  * @param description the description of the entity
  * @param key the status key
  * @param value the status value
  */
case class CurrentStatus(
  id: Option[Long],
  active: Boolean,
  description: String,
  key: String,
  value: String
) extends Common[CurrentStatus] {}
