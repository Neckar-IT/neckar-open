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
import assertk.assertions.isNotEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PhoneNumberTest {

  @Test
  fun `trims whitespace`() {
    assertThat(PhoneNumber("  +49 123 4567890  ").value).isEqualTo("+49 123 4567890")
  }

  @Test
  fun `collapses multiple spaces`() {
    assertThat(PhoneNumber("+49  123   456 7890").value).isEqualTo("+49 123 456 7890")
  }

  @Test
  fun `retains plus sign`() {
    assertThat(PhoneNumber("+491234567890").value).isEqualTo("+491234567890")
  }

  @Test
  fun `retains hyphens`() {
    assertThat(PhoneNumber("+49-123-4567890").value).isEqualTo("+49-123-4567890")
  }

  @Test
  fun `retains parentheses`() {
    assertThat(PhoneNumber("+49 (0) 123 4567890").value).isEqualTo("+49 (0) 123 4567890")
  }

  @Test
  fun `retains slashes`() {
    assertThat(PhoneNumber("07473/12345").value).isEqualTo("07473/12345")
  }

  @Test
  fun `strips non-phone characters`() {
    assertThat(PhoneNumber("Tel: +49 123 4567890!").value).isEqualTo("+49 123 4567890")
  }

  @Test
  fun `strips letters between digits and collapses the resulting gap`() {
    assertThat(PhoneNumber("+49 123 oder 4567890").value).isEqualTo("+49 123 4567890")
  }

  @Test
  fun `strips tabs and newlines - only the space is an allowed separator`() {
    assertThat(PhoneNumber("+49\t123\n4567890").value).isEqualTo("+491234567890")
  }

  @Test
  fun `normalizes combined input`() {
    assertThat(PhoneNumber("  +49  123  456 7890  ").value).isEqualTo("+49 123 456 7890")
  }

  @Test
  fun `spacing is part of the canonical form - differently spaced numbers are not equal`() {
    val compact = PhoneNumber("+491234567890")
    val spaced = PhoneNumber("+49 123 456 7890")

    assertThat(compact.value).isEqualTo("+491234567890")
    assertThat(spaced.value).isEqualTo("+49 123 456 7890")
    assertThat(compact).isNotEqualTo(spaced)
  }

  @Test
  fun `format returns the canonical value`() {
    assertThat(PhoneNumber("  +49  123  4567890  ").format()).isEqualTo("+49 123 4567890")
    assertThat(PhoneNumber("07473/12345").format()).isEqualTo("07473/12345")
  }

  @Test
  fun `rejects blank input`() {
    assertThrows<IllegalArgumentException> { PhoneNumber("   ") }
    assertThrows<IllegalArgumentException> { PhoneNumber("") }
  }

  @Test
  fun `rejects non-phone characters only`() {
    assertThrows<IllegalArgumentException> { PhoneNumber("abc") }
  }

  @Test
  fun `serialization roundtrip preserves canonical format`() {
    val phone = PhoneNumber("+49  123  4567890")
    val json = Json.encodeToString(phone)
    assertThat(json).isEqualTo("\"+49 123 4567890\"")

    val deserialized = Json.decodeFromString<PhoneNumber>(json)
    assertThat(deserialized).isEqualTo(phone)
  }

  @Test
  fun `deserialization normalizes input`() {
    val deserialized = Json.decodeFromString<PhoneNumber>("\"  +49  123  4567890  \"")
    assertThat(deserialized.value).isEqualTo("+49 123 4567890")
  }

  @Test
  fun `serializes as a property - present and null`() {
    val withFallback = ContactNumbers(PhoneNumber("+49  123  4567890"), PhoneNumber("07473/12345"))
    assertThat(Json.encodeToString(withFallback)).isEqualTo("""{"primary":"+49 123 4567890","fallback":"07473/12345"}""")
    assertThat(Json.decodeFromString<ContactNumbers>(Json.encodeToString(withFallback))).isEqualTo(withFallback)

    val withoutFallback = ContactNumbers(PhoneNumber("+49 123 4567890"), null)
    assertThat(Json.encodeToString(withoutFallback)).isEqualTo("""{"primary":"+49 123 4567890","fallback":null}""")
    assertThat(Json.decodeFromString<ContactNumbers>(Json.encodeToString(withoutFallback))).isEqualTo(withoutFallback)
  }

  @Test
  fun `deserialization rejects a blank value`() {
    assertThrows<IllegalArgumentException> { Json.decodeFromString<PhoneNumber>("\"  \"") }
  }

  @Test
  fun `toString returns canonical format`() {
    assertThat(PhoneNumber("+49  123  4567890").toString()).isEqualTo("+49 123 4567890")
  }

  @Test
  fun `examples are already in canonical form`() {
    assertThat(PhoneNumberExample.landlineWithTrunkPrefix.value).isEqualTo("+49 (0) 7071 555 0100")
    assertThat(PhoneNumberExample.mobile.value).isEqualTo("+49 170 1234567")
    assertThat(PhoneNumberExample.landline.value).isEqualTo("+49 7071 12345")
    assertThat(PhoneNumberExample.austria.value).isEqualTo("+43 1 234567")
    assertThat(PhoneNumberExample.switzerland.value).isEqualTo("+41 44 1234567")
  }
}

/**
 * Holder that mirrors how a phone number is used in a domain class: as a property and as a nullable property.
 */
@Serializable
private data class ContactNumbers(val primary: PhoneNumber, val fallback: PhoneNumber?)
