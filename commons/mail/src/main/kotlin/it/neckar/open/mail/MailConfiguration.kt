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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class MailConfiguration(
  val mailHost: String,
  val mailFrom: String,
  val mailPersonal: String,
  val smtpUser: String,
  val smtpPass: String,
  val mailSmtpAuth: String,
  /** Submission port: 587 with STARTTLS is what a mailbox provider expects, 25 carries no auth. */
  val smtpPort: Int = 587,
  /** Required, not merely enabled: opportunistic STARTTLS hands `AUTH LOGIN` to a downgrading MITM. */
  val startTlsRequired: Boolean = true,
  /** javax.mail waits forever by default, which turns a half-open connection into a wedged thread. */
  val connectTimeout: Duration = 30.seconds,
  val readTimeout: Duration = 30.seconds,
) {
  /** Keeps the password out of logs — the class sits on a request path. */
  override fun toString(): String = "MailConfiguration(mailHost=$mailHost, mailFrom=$mailFrom, smtpUser=$smtpUser, smtpPort=$smtpPort)"
}
