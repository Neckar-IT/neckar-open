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

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class StandardWebhooksVerifierTest {

  @Test
  fun `computes the signature of the Standard Webhooks test vector`() {
    assertThat(SpecSecret.computeSignatureEntry(SpecWebhookId, SpecTimestamp, SpecBody)).isEqualTo(SpecSignature)
  }

  @Test
  fun `accepts the Standard Webhooks test vector`() {
    assertThat(verifySpecDelivery(webhookSignature = SpecSignature)).isEqualTo(WebhookVerdict.Accepted)
  }

  @Test
  fun `accepts a header whose second entry matches`() {
    assertThat(verifySpecDelivery(webhookSignature = "v1,AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA= $SpecSignature")).isEqualTo(WebhookVerdict.Accepted)
  }

  @Test
  fun `rejects a signature made with another secret`() {
    val otherSecret = StandardWebhooksSecret("whsec_" + "B".repeat(43) + "=")
    val signature: String = otherSecret.computeSignatureEntry(SpecWebhookId, SpecTimestamp, SpecBody)

    assertThat(verifySpecDelivery(webhookSignature = signature)).isEqualTo(StandardWebhooksRejection.SignatureMismatch(SpecWebhookId))
  }

  @Test
  fun `rejects a modified body`() {
    assertThat(verifySpecDelivery(body = """{"test": 2432232315}""".toByteArray())).isEqualTo(StandardWebhooksRejection.SignatureMismatch(SpecWebhookId))
  }

  @Test
  fun `rejects an entry of another signature version`() {
    assertThat(verifySpecDelivery(webhookSignature = SpecSignature.replaceFirst("v1,", "v1a,"))).isEqualTo(StandardWebhooksRejection.SignatureMismatch(SpecWebhookId))
  }

  @Test
  fun `names each missing header`() {
    assertThat(verifySpecDelivery(webhookId = null)).isEqualTo(StandardWebhooksRejection.MissingWebhookId)
    assertThat(verifySpecDelivery(webhookTimestamp = null)).isEqualTo(StandardWebhooksRejection.MissingTimestamp(SpecWebhookId))
    assertThat(verifySpecDelivery(webhookSignature = null)).isEqualTo(StandardWebhooksRejection.MissingSignature(SpecWebhookId))
  }

  @Test
  fun `rejects a timestamp that is no number`() {
    assertThat(verifySpecDelivery(webhookTimestamp = "yesterday")).isEqualTo(StandardWebhooksRejection.MalformedTimestamp(SpecWebhookId, "yesterday"))
  }

  @Test
  fun `accepts a delivery at the edge of the window`() {
    assertThat(verifySpecDelivery(receivedAt = SpecSignedAt + 300.seconds)).isEqualTo(WebhookVerdict.Accepted)
    assertThat(verifySpecDelivery(receivedAt = SpecSignedAt - 300.seconds)).isEqualTo(WebhookVerdict.Accepted)
  }

  @Test
  fun `rejects a delivery outside the window`() {
    assertThat(verifySpecDelivery(receivedAt = SpecSignedAt + 301.seconds)).isEqualTo(StandardWebhooksRejection.TimestampOutOfWindow(SpecWebhookId, 301.seconds))
    assertThat(verifySpecDelivery(receivedAt = SpecSignedAt - 301.seconds)).isEqualTo(StandardWebhooksRejection.TimestampOutOfWindow(SpecWebhookId, (-301).seconds))
  }

  private val verifier = StandardWebhooksVerifier(SpecSecret)

  private fun verifySpecDelivery(
    webhookId: WebhookId? = SpecWebhookId,
    webhookTimestamp: String? = SpecTimestamp,
    webhookSignature: String? = SpecSignature,
    body: ByteArray = SpecBody,
    receivedAt: Instant = SpecSignedAt,
  ): WebhookVerdict {
    val headers: List<Pair<String, String?>> = listOf(
      "webhook-id" to webhookId?.value,
      "webhook-timestamp" to webhookTimestamp,
      "webhook-signature" to webhookSignature,
    )
    return verifier.verify({ name -> headers.firstOrNull { it.first == name }?.second }, body, receivedAt)
  }

  private companion object {
    // Test vector of https://github.com/standard-webhooks/standard-webhooks/blob/main/spec/standard-webhooks.md
    val SpecSecret: StandardWebhooksSecret = StandardWebhooksSecret("whsec_MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw")
    val SpecWebhookId: WebhookId = WebhookId("msg_p5jXN8AQM9LWM0D4loKWxJek")
    const val SpecTimestamp: String = "1614265330"
    val SpecBody: ByteArray = """{"test": 2432232314}""".toByteArray()
    const val SpecSignature: String = "v1,g0hM9SsE+OTPJTGt/tmIKtSyZlE3uFJELVlNIOLJ1OE="
    val SpecSignedAt: Instant = Instant.fromEpochSeconds(SpecTimestamp.toLong())
  }
}
