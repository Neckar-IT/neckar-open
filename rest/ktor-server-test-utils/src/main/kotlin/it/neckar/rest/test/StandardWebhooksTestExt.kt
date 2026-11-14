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
package it.neckar.rest.test

import io.ktor.client.request.*
import it.neckar.ktor.webhook.StandardWebhooksSecret
import it.neckar.ktor.webhook.StandardWebhooksVerifier
import it.neckar.ktor.webhook.WebhookId
import it.neckar.open.time.now

/**
 * Signs [body] the way a Standard Webhooks sender does and sets it as the request body.
 */
fun HttpRequestBuilder.signStandardWebhook(
  secret: StandardWebhooksSecret,
  body: String,
  webhookTimestamp: String = now().epochSeconds.toString(),
  webhookId: WebhookId = WebhookId("msg_test"),
) {
  header(StandardWebhooksVerifier.HeaderWebhookId, webhookId.value)
  header(StandardWebhooksVerifier.HeaderWebhookTimestamp, webhookTimestamp)
  header(StandardWebhooksVerifier.HeaderWebhookSignature, secret.computeSignatureEntry(webhookId, webhookTimestamp, body.toByteArray()))
  setBody(body)
}
