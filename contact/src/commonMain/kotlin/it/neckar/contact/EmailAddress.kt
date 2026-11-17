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
package it.neckar.contact

import it.neckar.open.kotlin.lang.ValueClassString
import kotlin.jvm.JvmInline
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A validated and normalized email address (ADL 0145).
 *
 * Normalizes input by trimming whitespace and lowercasing.
 * Performs basic structural validation: non-blank, contains exactly one `@`,
 * both local and domain parts non-empty.
 *
 * Example: `EmailAddress("  User@Example.COM  ")` produces `value = "user@example.com"`.
 */
@Serializable(with = EmailAddress.EmailAddressSerializer::class)
@JvmInline
value class EmailAddress private constructor(override val value: String) : ValueClassString {

  init {
    require(value.isNotBlank()) { "Email address must not be blank" }
    val atIndex = value.indexOf('@')
    require(atIndex > 0) { "Email address must contain '@' with a non-empty local part: $value" }
    require(atIndex < value.lastIndex) { "Email address must have a domain part after '@': $value" }
    require(value.indexOf('@', atIndex + 1) == -1) { "Email address must contain exactly one '@': $value" }
    // No rule against interior whitespace or control characters: stored documents predate one.
    // A caller that needs the stricter form enforces it at its own boundary.
  }

  override fun toString(): String = value

  /**
   * Returns the email address formatted for display to the end user.
   */
  fun format(): String = value

  companion object {
    /**
     * Creates an EmailAddress from any input format.
     * Trims whitespace and lowercases.
     */
    operator fun invoke(raw: String): EmailAddress {
      return EmailAddress(raw.trim().lowercase())
    }

    /** Whether [invoke] would accept this input. Derived from it, so the two cannot drift apart. */
    fun isValid(raw: String): Boolean = runCatching { invoke(raw) }.isSuccess
  }

  internal object EmailAddressSerializer : KSerializer<EmailAddress> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EmailAddress", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EmailAddress) {
      encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): EmailAddress {
      return invoke(decoder.decodeString())
    }
  }
}
