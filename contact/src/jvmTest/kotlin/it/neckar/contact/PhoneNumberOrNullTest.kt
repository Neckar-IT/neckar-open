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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class PhoneNumberOrNullTest {

  @Serializable
  private data class Holder(val phone: PhoneNumberOrNull)

  private fun decode(phoneJson: String): PhoneNumber? = Json.decodeFromString<Holder>("""{"phone":$phoneJson}""").phone

  @Test
  fun `empty string decodes to null`() {
    assertThat(decode("\"\"")).isNull()
  }

  @Test
  fun `whitespace only decodes to null`() {
    assertThat(decode("\"   \"")).isNull()
  }

  @Test
  fun `string without phone characters decodes to null`() {
    assertThat(decode("\"abc\"")).isNull()
  }

  @Test
  fun `json null decodes to null`() {
    assertThat(decode("null")).isNull()
  }

  @Test
  fun `value decodes like PhoneNumber invoke`() {
    assertThat(decode("\" +49  (7473) 12-3 \"")).isEqualTo(PhoneNumber("+49 (7473) 12-3"))
  }

  @Test
  fun `encode of null writes json null`() {
    assertThat(Json.encodeToString(Holder(null))).isEqualTo("""{"phone":null}""")
  }

  @Test
  fun `encode of value writes the string`() {
    assertThat(Json.encodeToString(Holder(PhoneNumber("+49 123")))).isEqualTo("""{"phone":"+49 123"}""")
  }
}
