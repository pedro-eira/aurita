package aurita.controllers.utility

/** Provides JSON serialisers/deserialisers for the task events. */
object TaskEventFormatter {
  import play.api.libs.json._
  import play.api.libs.json.Reads._
  import play.api.libs.functional.syntax._
  import play.api.mvc.WebSocket.MessageFlowTransformer
  import aurita.actors.utility.TaskProtocol._

  implicit val handshakeRequestReads: Reads[HandshakeRequest] =
    (__ \ "event").read[String].map {
      case "handshake" => HandshakeRequest()
    }

  implicit val handshakeRequestWrites: Writes[HandshakeRequest] =
    (__ \ "event").write[String].contramap(
      (handshake: HandshakeRequest) => "handshake"
    )

  implicit val handshakeResponseWrites: Writes[HandshakeResponse] = (
    (__ \ "event").write[String] ~
      (__ \ "status").write[MessageType.Value]
  ).apply(
    (handshake: HandshakeResponse) => ("handshake", handshake.status)
  )

  implicit val tickWrites: Writes[Tick] = (
    (__ \ "event").write[String].contramap(
      (tick: Tick) => "tick"
    )
  )

  implicit val tockWrites: Writes[Tock] = (
    (__ \ "event").write[String].contramap(
      (tock: Tock) => "tock"
    )
  )

  implicit val tockReads: Reads[Tock] =
    (__ \ "event").read[String].map {
      case "tock" => Tock()
    }

  implicit def taskEventFormat: Format[TaskEvent] = Format(
    (__ \ "event").read[String].flatMap {
      case "handshake" => handshakeRequestReads.map(identity)
      case "tock" => tockReads.map(identity)
      case other => Reads(_ => JsError("Unknown instance event: " + other))
    },
    Writes {
      case handshakeResponse: HandshakeResponse => {
        handshakeResponseWrites.writes(handshakeResponse)
      }
      case handshakeRequest: HandshakeRequest => {
        handshakeRequestWrites.writes(handshakeRequest)
      }
      case tick: Tick => tickWrites.writes(tick)
      case tock: Tock => tockWrites.writes(tock)
    }
  )

  /**
   * Formats WebSocket frames to be TaskEvents.
   */
  implicit def taskEventFrameFormatter: MessageFlowTransformer[
    TaskEvent, TaskEvent
  ] = MessageFlowTransformer.jsonMessageFlowTransformer[TaskEvent, TaskEvent]

}
