package aurita.daos.utility

import aurita.models.utility.Common
import scala.concurrent.Future
import slick.jdbc.JdbcProfile
import play.api.db.slick.HasDatabaseConfig
import scala.util.Try

trait CRUD[A <: Common[A]] {}

object DAOHelpersExceptions {
  case class IdException(message: String) extends Exception(message)

  case class RowNotFound(
    message: String
  ) extends Exception(message)
}

object DAOHelpers {}

trait DAOHelpers extends HasDatabaseConfig[JdbcProfile] {
  import DAOHelpersExceptions._
  import profile.api._
  import slick.sql.SqlProfile.ColumnOption.SqlType
  import scala.concurrent.ExecutionContext

  def DBType(typeInfo: String): SqlType = SqlType(typeInfo)

  abstract class RichTable[T](
    tag: Tag, name: String, idTypeInfo: String = ""
  )(implicit ec: ExecutionContext) extends Table[T](tag, name) {
    import java.sql.Timestamp

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc, DBType(idTypeInfo))

    def modifiedTimestamp = column[Option[Timestamp]](
      "MODIFIED_TIME", DBType("TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    )

    def createdTimestamp = column[Option[Timestamp]](
      "CREATED_TIME", DBType("TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    )

    def maybeUnapply =
      (_: Any) => throw new Exception(
        "Inserting into ? projection not supported."
      )
  }

  trait CRUDImpl[T <: RichTable[A], A <: Common[A]] extends CRUD[A] {}
}
