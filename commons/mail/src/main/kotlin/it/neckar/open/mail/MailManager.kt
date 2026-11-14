/*
 * Copyright (C) 2013-2026 Neckar IT GmbH, Mössingen, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting combined work under terms of your choice, provided that every
 * copy of the combined work is accompanied by a complete copy of the source
 * code of this library.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package it.neckar.open.mail

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class MailManager(
  private val configuration: MailConfiguration,
) {

  // Before [session]: initialisers run in declaration order, and a session built while this is
  // still null authenticates with nothing.
  val authenticator: Authenticator = object : Authenticator() {
    override fun getPasswordAuthentication(): PasswordAuthentication {
      return PasswordAuthentication(configuration.smtpUser, configuration.smtpPass)
    }
  }

  val session: Session = createSession()

  private fun createSession(): Session {
    return Session.getInstance(properties, authenticator)
  }

  /**
   * Creates a default message with the sender taken from the configuration.
   *
   * Throws when that sender cannot be used: a message without a sender is rejected by the transport,
   * far away from here and without naming the address that caused it.
   */
  fun createMessage(): MimeMessage {
    val message = MimeMessage(session)
    try {
      val from = InternetAddress(configuration.mailFrom)
      from.personal = configuration.mailPersonal
      message.setFrom(from)
    } catch (e: Exception) {
      throw IllegalStateException("Could not use the configured sender <${configuration.mailFrom}> with the personal name <${configuration.mailPersonal}>", e)
    }
    return message
  }

  /** On the calling thread and throwing, so a caller can record whether the mail arrived. */
  fun send(message: Message) {
    Transport.send(message)
  }

  /**
   * Connects and authenticates without sending, over the session's own properties — the same path
   * [send] takes. Credentials passed explicitly here would pass while every real send fails.
   */
  fun verifyConnection() {
    val transport = session.getTransport(smtpProtocol)
    try {
      transport.connect()
    } finally {
      // A failing close must not replace the connect failure.
      runCatching { transport.close() }.onFailure { logger.debug("Transport close failed", it) }
    }
  }

  private val properties: Properties
    get() {
      val properties = Properties()
      properties.setProperty(MailHost, configuration.mailHost)
      properties.setProperty(MailSmtpHost, configuration.mailHost)
      properties.setProperty(MailSmtpPort, configuration.smtpPort.toString())
      properties.setProperty(MailSmtpAuth, configuration.mailSmtpAuth)
      properties.setProperty(MailSmtpStarttls, configuration.startTlsRequired.toString())
      properties.setProperty(MailSmtpStarttlsRequired, configuration.startTlsRequired.toString())
      // Not tied to the STARTTLS flag: javax.mail defaults it to false, so a local relay without
      // TLS must not disarm identity checking for everyone after it.
      properties.setProperty(MailSmtpCheckServerIdentity, true.toString())
      // javax.mail enables only TLSv1, which every current JDK disables — the handshake then
      // fails with "No appropriate protocol" against any server.
      properties.setProperty(MailSmtpSslProtocols, tlsProtocols)
      properties.setProperty(MailSmtpConnectionTimeout, configuration.connectTimeout.inWholeMilliseconds.toString())
      properties.setProperty(MailSmtpTimeout, configuration.readTimeout.inWholeMilliseconds.toString())
      return properties
    }

  /**
   * The mail worker
   */
  private class MailWorker(private val message: Message) : Runnable {
    override fun run() {
      try {
        Transport.send(message)
      } catch (e: Exception) {
        logger.error("Could not send the message ${describeMessage()}", e)
      }
    }

    /**
     * Names the message the log line is about - the caller of [sendMail] is a thread that reports
     * nothing back, so this line is all there is. Reading the headers can fail on its own, and when
     * it does it must not replace the send failure it was meant to describe.
     */
    private fun describeMessage(): String = try {
      "to <${message.allRecipients?.joinToString() ?: "no recipients"}> with subject <${message.subject}>"
    } catch (e: MessagingException) {
      "whose headers could not be read ($e)"
    }
  }

  companion object {
    val logger: Logger = LoggerFactory.getLogger(MailManager::class.java)

    private const val MailHost = "mail.host"
    private const val MailSmtpHost = "mail.smtp.host"
    private const val MailSmtpPort = "mail.smtp.port"
    private const val MailSmtpAuth = "mail.smtp.auth"
    private const val MailSmtpStarttls = "mail.smtp.starttls.enable"
    private const val MailSmtpStarttlsRequired = "mail.smtp.starttls.required"
    private const val MailSmtpCheckServerIdentity = "mail.smtp.ssl.checkserveridentity"
    private const val MailSmtpSslProtocols = "mail.smtp.ssl.protocols"

    /** Space-separated, as javax.mail parses the property. */
    const val tlsProtocols: String = "TLSv1.2 TLSv1.3"
    private const val MailSmtpConnectionTimeout = "mail.smtp.connectiontimeout"
    private const val MailSmtpTimeout = "mail.smtp.timeout"
    private const val smtpProtocol = "smtp"

    @JvmStatic
    @Deprecated("Attention: Spawns a new thread!")
    fun sendMail(message: Message) {
      Thread(MailWorker(message)).start()
    }
  }
}
