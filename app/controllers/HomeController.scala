package aurita.controllers

import play.api.mvc.{
  AbstractController, Action, AnyContent, ControllerComponents, Request, WebSocket
}
import com.softwaremill.tagging.@@
import akka.actor.ActorSystem
import akka.stream.Materializer
import aurita.MainActorSystemTag
import aurita.actors.SocketClientFactory

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class HomeController(
  cc: ControllerComponents,
  socketClientFactory: SocketClientFactory,
  system: ActorSystem @@ MainActorSystemTag
)(implicit val materializer: Materializer) extends AbstractController(cc) {
  import play.api.libs.streams.ActorFlow
  import scala.concurrent.ExecutionContext

  implicit val ec = ExecutionContext.Implicits.global

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action {
    implicit request: Request[AnyContent] => Ok(views.html.index())
  }

  /**
   * Handle Websocket connection to the frontend user.
   *
   * @param userId the id of the frontend user connected to the channel
   */
  def stream() = {
    import aurita.actors.utility.TaskProtocol.TaskEvent
    import aurita.controllers.utility.TaskEventFormatter._

    WebSocket.accept[TaskEvent, TaskEvent] { _ => _getActorFlow(userId = 0L) }
  }

  private def _getActorFlow(userId: Long) = {
    implicit val actorSystem: ActorSystem @@ MainActorSystemTag = system
    ActorFlow.actorRef(out => socketClientFactory.socketClientProps(userId = userId))
  }

}
