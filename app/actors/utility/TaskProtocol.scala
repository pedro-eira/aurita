package aurita.actors.utility

/** Provides the Websocket messaging protocol between the backend, and frontend user.
  *
  */
object TaskProtocol {
  import scala.Enumeration

  /** The type of a Websocket message. */
  object MessageType extends Enumeration {
    type MessageType = Value
    val Error = Value
    val Warning = Value
    val Success = Value
    val Info = Value
    val Failure = Value
  }

  /** The events communicated through the client socket */
  sealed trait TaskEvent

   /** A Websocket connection response to the client.
    *
    * @constructor create a new response for a Websocket connection
    * @param status the status of the Websocket connection
    */
  case class HandshakeResponse(status: MessageType.Value) extends TaskEvent

   /** A Websocket connection request from the front end.
    *
    * @constructor create a new request for a Websocket connection
    */
  case class HandshakeRequest() extends TaskEvent

   /** A Websocket connection check request from the socket client to the frontend.
    *
    * @constructor create a new connection check request for the channel.
    */
  case class Tick() extends TaskEvent

   /** A Websocket connection check response from the frontend to the socket client.
    *
    * @constructor create a new connection check response for the channel
    */
  case class Tock() extends TaskEvent
}
