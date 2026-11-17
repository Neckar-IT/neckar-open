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
package it.neckar.ktor.webhook

import java.security.MessageDigest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Checks the HMAC-SHA256 signature of the Standard Webhooks specification (https://www.standardwebhooks.com/).
 */
class StandardWebhooksVerifier(
  private val secret: StandardWebhooksSecret,
) : WebhookVerifier {

  override fun verify(headerValue: (String) -> String?, body: ByteArray, receivedAt: Instant): WebhookVerdict {
    val webhookId: WebhookId = headerValue(HeaderWebhookId)?.let { WebhookId(it) } ?: return StandardWebhooksRejection.MissingWebhookId
    val webhookTimestamp: String = headerValue(HeaderWebhookTimestamp) ?: return StandardWebhooksRejection.MissingTimestamp(webhookId)
    val webhookSignature: String = headerValue(HeaderWebhookSignature) ?: return StandardWebhooksRejection.MissingSignature(webhookId)

    val signedAtEpochSeconds: Long = webhookTimestamp.toLongOrNull() ?: return StandardWebhooksRejection.MalformedTimestamp(webhookId, webhookTimestamp)
    val skew: Duration = (receivedAt.epochSeconds - signedAtEpochSeconds).seconds
    if (skew.absoluteValue > TimestampTolerance) return StandardWebhooksRejection.TimestampOutOfWindow(webhookId, skew)

    val expected: ByteArray = secret.computeSignatureEntry(webhookId, webhookTimestamp, body).toByteArray(Charsets.UTF_8)
    // Constant-time comparison per entry; an entry of another version can never equal a `v1,` entry.
    val matches: Boolean = webhookSignature.split(' ').any { entry -> MessageDigest.isEqual(expected, entry.toByteArray(Charsets.UTF_8)) }
    return if (matches) WebhookVerdict.Accepted else StandardWebhooksRejection.SignatureMismatch(webhookId)
  }

  companion object {
    const val HeaderWebhookId: String = "webhook-id"
    const val HeaderWebhookTimestamp: String = "webhook-timestamp"
    const val HeaderWebhookSignature: String = "webhook-signature"

    /**
     * The Standard Webhooks default: wide enough for clock drift and the sender's queue, narrow enough to bound a replay.
     */
    val TimestampTolerance: Duration = 5.minutes
  }
}
