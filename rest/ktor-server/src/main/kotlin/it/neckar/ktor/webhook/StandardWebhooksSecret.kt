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

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.jvm.JvmInline

/**
 * The shared HMAC key of Standard Webhooks in its `whsec_<base64>` form.
 */
@JvmInline
value class StandardWebhooksSecret(val value: String) {
  init {
    require(value.startsWith(Prefix)) { "A webhook secret starts with <$Prefix>" }
    val keyLength: Int = decodeKey().size
    require(keyLength in KeyLengthRange) { "A webhook secret encodes $KeyLengthRange bytes, this one $keyLength" }
  }

  /**
   * The `v1,<base64>` entry of the `webhook-signature` header for one delivery.
   */
  fun computeSignatureEntry(webhookId: WebhookId, webhookTimestamp: String, body: ByteArray): String {
    val mac: Mac = Mac.getInstance(hmacAlgorithm)
    mac.init(SecretKeySpec(decodeKey(), hmacAlgorithm))
    mac.update("$webhookId.$webhookTimestamp.".toByteArray(Charsets.UTF_8))
    return "$SignatureVersion,${Base64.getEncoder().encodeToString(mac.doFinal(body))}"
  }

  private fun decodeKey(): ByteArray = Base64.getDecoder().decode(value.removePrefix(Prefix))

  override fun toString(): String = "$Prefix***"

  companion object {
    const val Prefix: String = "whsec_"

    /**
     * Standard Webhooks names HMAC-SHA256 `v1`.
     */
    const val SignatureVersion: String = "v1"

    /**
     * The range Standard Webhooks allows.
     */
    val KeyLengthRange: IntRange = 24..64

    private const val hmacAlgorithm: String = "HmacSHA256"
  }
}
