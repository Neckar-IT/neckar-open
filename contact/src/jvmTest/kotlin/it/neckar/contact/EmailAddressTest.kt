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

class EmailAddressTest {

  @Test
  fun `valid email addresses`() {
    assertThat(EmailAddress("user@example.com").value).isEqualTo("user@example.com")
    assertThat(EmailAddress("a@b.c").value).isEqualTo("a@b.c")
    assertThat(EmailAddress("first.last@domain.org").value).isEqualTo("first.last@domain.org")
    assertThat(EmailAddress("user+tag@sub.example.co.uk").value).isEqualTo("user+tag@sub.example.co.uk")
    assertThat(EmailAddress("user_name@example-domain.com").value).isEqualTo("user_name@example-domain.com")
    assertThat(EmailAddress("123@456.789").value).isEqualTo("123@456.789")
  }

  /**
   * What [EmailAddress.isValid] accepts, the constructor takes. Each verdict is spelled out rather
   * than compared against the constructor, which would assert `f(x) == f(x)`.
   */
  @Test
  fun `isValid accepts exactly what the constructor accepts`() {
    val expected = mapOf(
      "user@example.com" to true,
      "a@b.c" to true,
      "first.last@domain.org" to true,
      "user+tag@sub.example.co.uk" to true,
      "  User@Example.COM  " to true,
      "\t user@example.com \n" to true,
      "" to false,
      "   " to false,
      "no-at-sign" to false,
      "@example.com" to false,
      "user@" to false,
      "a@b@c" to false,
      // Stored documents predate a rule against these; the leads endpoint rejects them itself.
      "a\r\nBcc: victim@example.com" to true,
      "a b@example.com" to true,
      "a\u0000b@example.com" to true,
    )

    expected.forEach { (candidate, valid) ->
      assertThat(EmailAddress.isValid(candidate), "isValid for <$candidate>").isEqualTo(valid)
      assertThat(runCatching { EmailAddress(candidate) }.isSuccess, "constructor for <$candidate>").isEqualTo(valid)
    }
  }

  /** A migration wrote free-text contact values here, so a stricter rule would make those documents unreadable. */
  @Test
  fun `keeps reading addresses a stricter rule would reject`() {
    assertThat(EmailAddress("Max Mustermann max@example.com").value).isEqualTo("max mustermann max@example.com")
  }

  @Test
  fun `normalizes to lowercase`() {
    assertThat(EmailAddress("User@Example.COM").value).isEqualTo("user@example.com")
    assertThat(EmailAddress("ADMIN@DOMAIN.ORG").value).isEqualTo("admin@domain.org")
  }

  @Test
  fun `trims whitespace`() {
    assertThat(EmailAddress("  user@example.com  ").value).isEqualTo("user@example.com")
  }

  @Test
  fun `trims tabs and newlines`() {
    assertThat(EmailAddress("\t user@example.com \n").value).isEqualTo("user@example.com")
  }

  @Test
  fun `normalizes combined input`() {
    assertThat(EmailAddress("  User@Example.COM  ").value).isEqualTo("user@example.com")
  }

  @Test
  fun `different input formats produce equal instances`() {
    val lowercase = EmailAddress("user@example.com")
    val uppercase = EmailAddress("USER@EXAMPLE.COM")
    val trimmed = EmailAddress("  user@example.com  ")

    assertThat(lowercase).isEqualTo(uppercase)
    assertThat(lowercase).isEqualTo(trimmed)
    assertThat(lowercase).isNotEqualTo(EmailAddress("other@example.com"))
  }

  @Test
  fun `toString returns the email value`() {
    assertThat(EmailAddress("User@Example.COM").toString()).isEqualTo("user@example.com")
  }

  @Test
  fun `format returns the canonical value`() {
    assertThat(EmailAddress("  User@Example.COM  ").format()).isEqualTo("user@example.com")
    assertThat(EmailAddress("info@example.com").format()).isEqualTo("info@example.com")
  }

  @Test
  fun `rejects blank`() {
    assertThrows<IllegalArgumentException> { EmailAddress("") }
    assertThrows<IllegalArgumentException> { EmailAddress("  ") }
  }

  @Test
  fun `rejects missing at sign`() {
    assertThrows<IllegalArgumentException> { EmailAddress("userexample.com") }
  }

  @Test
  fun `rejects empty local part`() {
    assertThrows<IllegalArgumentException> { EmailAddress("@example.com") }
  }

  @Test
  fun `rejects empty domain part`() {
    assertThrows<IllegalArgumentException> { EmailAddress("user@") }
  }

  @Test
  fun `rejects multiple at signs`() {
    assertThrows<IllegalArgumentException> { EmailAddress("user@@example.com") }
    assertThrows<IllegalArgumentException> { EmailAddress("user@ex@ample.com") }
  }

  @Test
  fun `serialization roundtrip preserves canonical format`() {
    val email = EmailAddress("User@Example.COM")
    val json = Json.encodeToString(email)
    assertThat(json).isEqualTo("\"user@example.com\"")

    val deserialized = Json.decodeFromString<EmailAddress>(json)
    assertThat(deserialized).isEqualTo(email)
  }

  @Test
  fun `deserialization normalizes input`() {
    val deserialized = Json.decodeFromString<EmailAddress>("\"  USER@EXAMPLE.COM  \"")
    assertThat(deserialized.value).isEqualTo("user@example.com")
  }

  @Test
  fun `deserialization rejects a structurally invalid value`() {
    assertThrows<IllegalArgumentException> { Json.decodeFromString<EmailAddress>("\"userexample.com\"") }
    assertThrows<IllegalArgumentException> { Json.decodeFromString<EmailAddress>("\"\"") }
  }

  @Test
  fun `serializes as a property - present and null`() {
    val withPersonal = ContactAddresses(EmailAddress("Info@Example.COM"), EmailAddress("Max.Mustermann@Example.DE"))
    assertThat(Json.encodeToString(withPersonal)).isEqualTo("""{"business":"info@example.com","personal":"max.mustermann@example.de"}""")
    assertThat(Json.decodeFromString<ContactAddresses>(Json.encodeToString(withPersonal))).isEqualTo(withPersonal)

    val withoutPersonal = ContactAddresses(EmailAddress("info@example.com"), null)
    assertThat(Json.encodeToString(withoutPersonal)).isEqualTo("""{"business":"info@example.com","personal":null}""")
    assertThat(Json.decodeFromString<ContactAddresses>(Json.encodeToString(withoutPersonal))).isEqualTo(withoutPersonal)
  }

  @Test
  fun `examples are already in canonical form`() {
    assertThat(EmailAddress.Example.info.value).isEqualTo("info@example.com")
    assertThat(EmailAddress.Example.support.value).isEqualTo("support@example.com")
    assertThat(EmailAddress.Example.personal.value).isEqualTo("max.mustermann@example.de")
    assertThat(EmailAddress.Example.admin.value).isEqualTo("admin@example.org")
  }
}

/**
 * Holder that mirrors how an email address is used in a domain class: as a property and as a nullable property.
 */
@Serializable
private data class ContactAddresses(val business: EmailAddress, val personal: EmailAddress?)
