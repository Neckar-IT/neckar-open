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
import org.junit.jupiter.api.Test

class PersonDisplayNameTest {

  @Test
  fun `joins first and last name`() {
    assertThat(personDisplayName("Alice", "Anderson", Email)).isEqualTo("Alice Anderson")
  }

  @Test
  fun `uses the only present name part`() {
    assertThat(personDisplayName("Alice", null, Email)).isEqualTo("Alice")
    assertThat(personDisplayName(null, "Anderson", Email)).isEqualTo("Anderson")
  }

  @Test
  fun `falls back to the email when no name is present`() {
    assertThat(personDisplayName(null, null, Email)).isEqualTo("alice@example.com")
  }

  @Test
  fun `skips blank name parts`() {
    assertThat(personDisplayName(" ", "", Email)).isEqualTo("alice@example.com")
    assertThat(personDisplayName("", " Anderson ", Email)).isEqualTo("Anderson")
  }

  private companion object {
    val Email: EmailAddress = EmailAddress("alice@example.com")
  }
}
