package aurita.actors

import akka.actor.{ Props, Actor }

abstract class SocketClientFactory {
  def socketClientProps(userId: Long): Props
}

/** Companion to the [[SocketClient Websocket client]].
  *
  */
class SocketClientFactoryImpl() extends SocketClientFactory {
  import com.softwaremill.macwire.wire

  def socketClientProps(userId: Long): Props = Props(wire[SocketClient])
}

/** The actor receiving messages from the [[aurita.controllers.HomeController.stream Websocket]] connection
  * to the frontend user
  *
  * @constructor create a new channel actor to receive requests from the frontend user
  * @param userId the id of the frontend user connected to the channel
  */
class SocketClient(userId: Long) extends Actor {
  import play.api.libs.json.Json
  import aurita.actors.utility.TaskProtocol.{
    HandshakeResponse, HandshakeRequest, Tick, Tock
  }

  lazy val logger = new aurita.utility.messages.Log(this.getClass)

  def receive = active

  def active(): Receive = {
    case tick: Tick => logger.debug(s"${self.path} received ${tick}")

    case tock: Tock => logger.debug(s"${self.path} received ${tock}")

    case handshake: HandshakeRequest => logger.debug(
      s"${self.path} received ${handshake}"
    )

    case handshake: HandshakeResponse => logger.debug(
      s"${self.path} received handshake response = ${handshake}"
    )

    case msg: Any => {
      logger.debug(s"${self.path} received message: $msg.")
    }
  }
}
