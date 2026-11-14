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

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

class MailManagerTest {
  private val configuration = MailConfiguration(
    mailHost = "mail.example.com",
    mailFrom = "sender@example.com",
    mailPersonal = "Neckar IT",
    smtpUser = "user",
    smtpPass = "pass",
    mailSmtpAuth = "true",
  )

  /** A session built before the authenticator field captures null, and every send reaches the server unauthenticated. */
  @Test
  fun `the session carries the authenticator`() {
    val session = MailManager(configuration).session

    val authentication = session.requestPasswordAuthentication(null, -1, "smtp", null, null)
    assertThat(authentication).isNotNull()
    assertThat(authentication.userName).isEqualTo("user")
    assertThat(authentication.password).isEqualTo("pass")
  }

  @Test
  fun `configures the submission port, STARTTLS and timeouts`() {
    val properties = MailManager(configuration).session.properties

    assertThat(properties.getProperty("mail.smtp.host")).isEqualTo("mail.example.com")
    assertThat(properties.getProperty("mail.smtp.port")).isEqualTo("587")
    assertThat(properties.getProperty("mail.smtp.auth")).isEqualTo("true")
    assertThat(properties.getProperty("mail.smtp.connectiontimeout")).isEqualTo("30000")
    assertThat(properties.getProperty("mail.smtp.timeout")).isEqualTo("30000")
  }

  /** `starttls.enable` alone is opportunistic and `checkserveridentity` defaults to false — either hands out the password. */
  @Test
  fun `requires STARTTLS and verifies the certificate identity`() {
    val properties = MailManager(configuration).session.properties

    assertThat(properties.getProperty("mail.smtp.starttls.enable")).isEqualTo("true")
    assertThat(properties.getProperty("mail.smtp.starttls.required")).isEqualTo("true")
    assertThat(properties.getProperty("mail.smtp.ssl.checkserveridentity")).isEqualTo("true")
  }

  /** javax.mail enables only TLSv1, which every current JDK disables — the handshake fails against any server. */
  @Test
  fun `enables the TLS versions a current JDK still accepts`() {
    val properties = MailManager(configuration).session.properties

    assertThat(properties.getProperty("mail.smtp.ssl.protocols")).isEqualTo("TLSv1.2 TLSv1.3")
  }

  /** Turning TLS off for a local relay must not disarm identity checking for everyone after it. */
  @Test
  fun `verifies the certificate identity even without STARTTLS`() {
    val withoutTls = MailManager(configuration.copy(startTlsRequired = false)).session.properties

    assertThat(withoutTls.getProperty("mail.smtp.starttls.required")).isEqualTo("false")
    assertThat(withoutTls.getProperty("mail.smtp.ssl.checkserveridentity")).isEqualTo("true")
  }

  /** The configuration sits on a request path, so its password must not reach a log line. */
  @Test
  fun `does not print the password`() {
    assertThat(configuration.toString()).doesNotContain("pass")
  }

  @Test
  fun `sets the configured sender`() {
    val message = MailManager(configuration).createMessage()

    val from = message.from.single() as InternetAddress
    assertThat(from.address).isEqualTo("sender@example.com")
    assertThat(from.personal).isEqualTo("Neckar IT")
  }

  /** Handed to the transport, the failure no longer names the address that caused it. */
  @Test
  fun `names the rejected sender address`() {
    val misconfigured = configuration.copy(mailFrom = "not an address")

    val exception = assertThrows<IllegalStateException> {
      MailManager(misconfigured).createMessage()
    }

    assertThat(exception.message).isNotNull().contains("not an address")
    assertThat(exception.cause).isNotNull().isInstanceOf(AddressException::class)
  }
}
