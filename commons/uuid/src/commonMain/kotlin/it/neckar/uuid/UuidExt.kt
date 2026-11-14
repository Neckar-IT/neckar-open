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
package it.neckar.uuid

import it.neckar.open.annotations.JsExportForTs
import it.neckar.open.annotations.JsFriendly
import it.neckar.open.annotations.TestOnly
import it.neckar.open.kotlin.lang.fromBase64Url
import it.neckar.open.kotlin.lang.random
import it.neckar.open.kotlin.lang.toBase64UrlString
import it.neckar.runtime.context.RuntimeContext
import kotlin.uuid.Uuid


/**
 * Returns a new UUID by combining the bytes of both given UUIDs using XOR.
 *
 * ATTENTION: This is not a secure way to combine UUIDs. Only use for testing.
 */
infix fun Uuid.xor(other: Uuid): Uuid {
  other.toLongs { mostSignificantLong, leastSignificantLong ->
    return this.xor(mostSignificantLong = mostSignificantLong, leastSignificantLong = leastSignificantLong)
  }
}


/**
 * Creates a new UUID using XOR.
 *
 * ATTENTION: This is not a secure way to combine UUIDs. Only use for testing.
 */
fun Uuid.xor(mostSignificantLong: Long, leastSignificantLong: Long): Uuid {
  this.toLongs { mostSignificantBits, leastSignificantBits ->
    return Uuid.fromLongs(mostSignificantBits.xor(mostSignificantLong), leastSignificantBits.xor(leastSignificantLong))
  }
}

/**
 * Adds the given ordinal to the given UUID.
 *
 * WARNING: Do not use hashCode because it may be platform-dependent (e.g. Double)
 *
 * ATTENTION: Should not be used for production.
 * This generates predicable UUIDs - which is usually not what you want.
 */
operator fun Uuid.plus(additionalValue: Int): Uuid {
  this.toLongs { mostSignificantBits, leastSignificantBits ->
    return Uuid.fromLongs(mostSignificantBits, leastSignificantBits + additionalValue)
  }
}

/**
 * Adds a custom value depending on the *name* of the enum.
 *
 * ATTENTION: Should not be used for production.
 */
operator fun <E : Enum<E>> Uuid.plus(additionalEnumValue: E): Uuid {
  return this.plus(additionalEnumValue.name.encodeToByteArray().sum())
}

/**
 * Returns a new UUID using the pseudo random generator.
 *
 * ATTENTION: Do *not* use for production. Use [Uuid.random] instead!
 */
@TestOnly
fun Uuid.Companion.pseudoRandom(): Uuid {
  // Force the RFC-4122 v4 version (0x4 nibble) and variant (10xx) bits so test-generated ids are
  // valid v4 UUIDs, identical in shape to production ids from Uuid.random(). Without this masking
  // the raw random longs leave the version/variant nibbles arbitrary, producing 128-bit values that
  // are not RFC-conformant and that strict validators (e.g. frontend z.uuid()) reject.
  val mostSignificantBits = (random.nextLong() and 0xF000L.inv()) or 0x4000L
  val leastSignificantBits = (random.nextLong() and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE
  return fromLongs(mostSignificantBits, leastSignificantBits)
}

/**
 * Returns encoded URL safe Base64
 *
 * @see decodeFromUrl
 */
fun Uuid.encodeForUrl(): String {
  val toUrlBase64 = toByteArray().toBase64UrlString()
  //Base64 adds padding - with at most two "=".
  return toUrlBase64
    .removeSuffix("=")
    .removeSuffix("=")
}

/**
 * Decodes URL safe Base64 back to original Uuid
 *
 * @see encodeForUrl
 **/
fun Uuid.Companion.decodeFromUrl(uuidEncodedForUrl: String): Uuid {
  val bytes = uuidEncodedForUrl.fromBase64Url()
  return fromByteArray(bytes)
}

// Deprecated functions

/**
 * Creates a new random UUID.
 *
 * The random generator can be configured for tests.
 */
@JsFriendly
@JsExportForTs
fun randomUuid4(): Uuid {
  return Uuid.randomForEnvironment()
}

/**
 * Parses a UUID string
 */
@JsFriendly
@JsExportForTs
fun parseUuid(uuidAsString: String): Uuid {
  return Uuid.parse(uuidAsString)
}

/**
 * Returns the hex string
 */
@JsFriendly
@JsExportForTs
fun formatUuid(uuid: Uuid): String {
  return uuid.toHexDashString()
}

/**
 * Creates a new random UUID.
 */
fun Uuid.Companion.randomForEnvironment(): Uuid {
  if (RuntimeContext.inUnitTest) {
    return Uuid.pseudoRandom()
  }
  return random()
}

/**
 * Use `Uuid.parse()` instead
 */
@Deprecated("Inline", ReplaceWith("Uuid.parse(string)", "kotlin.uuid.Uuid"))
inline fun uuidFrom(string: String): Uuid {
  return Uuid.parse(string)
}

@Deprecated("Inline", ReplaceWith("Uuid.parse(uuidString)", "kotlin.uuid.Uuid"))
inline fun Uuid.Companion.fromString(uuidString: String): Uuid {
  return Uuid.parse(uuidString)
}

/**
 * Returns the bytes of the UUID.
 */
@Deprecated("Inline!", ReplaceWith("this.toByteArray()"))
inline fun Uuid.bytes(): ByteArray {
  return this.toByteArray()
}

/**
 * Returns the bytes of the UUID.
 */
@Deprecated("Inline!", ReplaceWith("this.toByteArray()"))
val Uuid.bytes: ByteArray
  get() {
    return this.toByteArray()
  }

/**
 * Creates a new UUID from the given bytes.
 * The given byte array must have a length of 16.
 */
@Deprecated("Inline", ReplaceWith("Uuid.fromByteArray(bytes)", "kotlin.uuid.Uuid"))
fun uuidOf(bytes: ByteArray): Uuid {
  return Uuid.fromByteArray(bytes)
}

