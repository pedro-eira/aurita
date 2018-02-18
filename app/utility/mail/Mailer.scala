package aurita.utility.mail

import com.softwaremill.tagging._
import akka.actor.ActorSystem
import play.api.libs.mailer.MailerClient
import scala.concurrent.ExecutionContext
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.Configuration
import aurita.MainActorSystemTag
import play.api.i18n.MessagesProvider

abstract class Mailer {
  def welcome(email: String, name: String, link: String)(
    implicit messagesProvider: MessagesProvider
  ): Unit

  def postSignUpWelcome(email: String, name: String)(
    implicit messagesProvider: MessagesProvider
  ): Unit

  def forgotPassword(
    email: String, name: String, link: String
  )(implicit messagesProvider: MessagesProvider): Unit

  def error (name: String, mesg: String)(
    implicit messagesProvider: MessagesProvider
  ): Unit
}

class MailerImpl(
  val messagesApi: MessagesApi,
  mailerClient: MailerClient,
  system: ActorSystem @@ MainActorSystemTag
)(implicit ec: ExecutionContext) extends Mailer
  with I18nSupport {
  import scala.language.postfixOps
  import play.api.Play.current
  import views.html.auth.mails
  import play.twirl.api.Html
  import scala.language.implicitConversions
  import play.api.libs.concurrent.Akka
  import scala.concurrent.duration._
  import play.api.libs.mailer.Email
  import com.typesafe.config.ConfigFactory

  val from: String = ConfigFactory.load().getString("mail.admin")

  private def _sendEmailAsync(recipients: String*)(
    subject: String, bodyHtml: String, bodyText: String = ""
  ): Unit = {
    system.scheduler.scheduleOnce(100 milliseconds) {
      _sendEmail(recipients: _*)(subject, bodyHtml, bodyText)
    }
  }

  private def _sendEmail(recipients: String*)(
    subject: String, bodyHtml: String, bodyText: String = ""
  ): Unit = {
    val email = Email(
      subject,
      from,
      recipients,
      bodyText = Some(bodyText),
      bodyHtml = Some(bodyHtml)
    )
    mailerClient.send(email)
  }

  implicit def html2String(html: Html): String = html.toString

  def welcome(email: String, name: String, link: String)(
    implicit messagesProvider: MessagesProvider
  ): Unit = {
    _sendEmailAsync(email)(
      subject = Messages("mail.welcome.subject"),
      bodyHtml = mails.welcome(name, link),
      bodyText = mails.welcomeTxt(name, link)
    )
  }

  def postSignUpWelcome(email: String, name: String)(
    implicit messagesProvider: MessagesProvider
  ): Unit = {
    _sendEmailAsync(email)(
      subject = Messages("mail.postSignUpWelcome.subject", name),
      bodyHtml = mails.postSignUpWelcome(name),
      bodyText = mails.postSignUpWelcomeTxt(name)
    )
  }

  def error(name: String, mesg: String)(
    implicit messagesProvider: MessagesProvider
  ): Unit = {
    _sendEmailAsync(from)(
      subject = Messages("mail.error.subject", name),
      bodyHtml = mails.error(name, mesg),
      bodyText = mails.errorTxt(name, mesg)
    )
  }

  def forgotPassword(
    email: String, name: String, link: String
  )(implicit messagesProvider: MessagesProvider): Unit = {
     _sendEmailAsync(email)(
      subject = Messages("mail.forgotpasswd.subject"),
      bodyHtml = mails.forgotPassword(name, link),
      bodyText = mails.forgotPasswordTxt(name, link)
    )
  }
}