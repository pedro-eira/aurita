import java.io.File
import java.net.InetSocketAddress
import play.sbt.PlayRunHook
import sbt._

object WebpackServer {
  import scala.sys.process

  def apply(base: File): PlayRunHook = {
    object WebpackServerScript extends PlayRunHook {
      // This is really ugly, how can I do this functionally?
      private var _process: Option[process.Process] = None

      override def afterStarted(addr: InetSocketAddress): Unit = {
        _process = Option(process.Process("npm run startWebpack", base).run)
      }

      override def afterStopped(): Unit = {
        _process.map(_.destroy)
        _process = None
      }
    }
    WebpackServerScript
  }
}