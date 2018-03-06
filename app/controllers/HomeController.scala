package aurita.controllers

import play.api.mvc.{
  AbstractController, Action, AnyContent, ControllerComponents, Request, WebSocket
}
import play.api.Environment
import play.api.libs.ws.WSClient
import com.softwaremill.tagging.@@
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mohiva.play.silhouette.api.Silhouette
import scala.concurrent.ExecutionContext
import controllers.Assets
import aurita.MainActorSystemTag
import aurita.actors.SocketClientFactory
import aurita.utility.auth.DefaultEnv

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class HomeController(
  assets: Assets,
  cc: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  socketClientFactory: SocketClientFactory,
  system: ActorSystem @@ MainActorSystemTag,
  environment: Environment,
  ws: WSClient
)(
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends AbstractController(cc) {
  import play.api.libs.streams.ActorFlow
  import play.api.Mode
  import controllers.Assets
  import scala.concurrent.Future

  def bundle(file:String) = environment.mode match {
    case Mode.Dev => Action.async {
      ws.url("http://localhost:8080/bundles/" + file).get().map{
        response => Ok(response.body).as("text/javascript")
      }
    }
    // If Production, use build files.
    case Mode.Prod => assets.at("public/javascripts", file)
    case Mode.Test => assets.at("public/javascripts", file)
  }

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(pathname: String) = silhouette.UserAwareAction.async {
    implicit request: Request[AnyContent] => Future { Ok(views.html.index()) }
  }

  def index0() = index(pathname = "")

  def index1() = silhouette.UserAwareAction.async {
    implicit request: Request[AnyContent] => Future {
      Redirect(aurita.controllers.routes.HomeController.index0)
    }
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
