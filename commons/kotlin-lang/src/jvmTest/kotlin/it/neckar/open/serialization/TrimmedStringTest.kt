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
package it.neckar.open.serialization

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class TrimmedStringTest {
  @Serializable
  private data class TrimmedHolder(val value: TrimmedString)

  @Serializable
  private data class NullableHolder(val value: TrimmedStringOrNull = null)

  @Test
  fun `trims surrounding whitespace on decode`() {
    val decoded = Json.decodeFromString<TrimmedHolder>("""{"value":"  Anna  "}""")
    assertThat(decoded.value).isEqualTo("Anna")
  }

  @Test
  fun `trimmed string allows blank values`() {
    val decoded = Json.decodeFromString<TrimmedHolder>("""{"value":"   "}""")
    assertThat(decoded.value).isEqualTo("")
  }

  @Test
  fun `trimmed string keeps inner whitespace`() {
    val decoded = Json.decodeFromString<TrimmedHolder>("""{"value":"  Anna  Müller  "}""")
    assertThat(decoded.value).isEqualTo("Anna  Müller")
  }

  @Test
  fun `encode of TrimmedString is unchanged`() {
    val encoded = Json.encodeToString(TrimmedHolder.serializer(), TrimmedHolder("Anna"))
    assertThat(encoded).isEqualTo("""{"value":"Anna"}""")
  }

  @Test
  fun `nullable trimmed string trims valid input`() {
    val decoded = Json.decodeFromString<NullableHolder>("""{"value":"  Anna  "}""")
    assertThat(decoded.value).isEqualTo("Anna")
  }

  @Test
  fun `nullable trimmed string collapses whitespace-only to null`() {
    val decoded = Json.decodeFromString<NullableHolder>("""{"value":"   "}""")
    assertThat(decoded.value).isNull()
  }

  @Test
  fun `nullable trimmed string collapses empty string to null`() {
    val decoded = Json.decodeFromString<NullableHolder>("""{"value":""}""")
    assertThat(decoded.value).isNull()
  }

  @Test
  fun `nullable trimmed string keeps explicit null`() {
    val decoded = Json.decodeFromString<NullableHolder>("""{"value":null}""")
    assertThat(decoded.value).isNull()
  }

  @Test
  fun `nullable trimmed string keeps default null when property is absent`() {
    val decoded = Json.decodeFromString<NullableHolder>("""{}""")
    assertThat(decoded.value).isNull()
  }

  @Test
  fun `encode of TrimmedStringOrNull preserves null`() {
    val explicitNulls = Json { encodeDefaults = true; explicitNulls = true }
    val encoded = explicitNulls.encodeToString(NullableHolder.serializer(), NullableHolder(null))
    assertThat(encoded).isEqualTo("""{"value":null}""")
  }

  @Test
  fun `encode of TrimmedStringOrNull preserves value`() {
    val encoded = Json.encodeToString(NullableHolder.serializer(), NullableHolder("Anna"))
    assertThat(encoded).isEqualTo("""{"value":"Anna"}""")
  }
}
