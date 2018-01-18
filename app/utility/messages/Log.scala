package aurita.utility.messages

class Log(classVal: Class[_]) {
  import play.api.libs.json.Json

  private val _logger = play.api.Logger(classVal)

  def debug(mesg: => String) = _logger.debug(mesg)

  def error(mesg: => String) = _logger.error(mesg)

  def error(mesg: => String, error: => Throwable) = _logger.error(mesg, error)

  def info(mesg: => String) = _logger.info(mesg)

  def info(mesg: => String, error: => Throwable) = _logger.info(mesg, error)

  def warn(mesg: => String) = _logger.warn(mesg)

  def warn(mesg: => String, error: => Throwable) = _logger.warn(mesg, error)

}
