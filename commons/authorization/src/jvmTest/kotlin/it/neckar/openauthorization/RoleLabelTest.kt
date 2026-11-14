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
package it.neckar.openauthorization

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import it.neckar.open.authorization.RoleLabel
import org.junit.jupiter.api.Test

class RoleLabelTest {
  @Test
  fun `accepts predefined slug values`() {
    assertThat(RoleLabel("owner").value).isEqualTo("owner")
    assertThat(RoleLabel("admin").value).isEqualTo("admin")
  }

  @Test
  fun `accepts user-entered labels with whitespace, umlauts, and mixed case`() {
    assertThat(RoleLabel("Buchhalter Pro").value).isEqualTo("Buchhalter Pro")
    assertThat(RoleLabel("Außendienst").value).isEqualTo("Außendienst")
  }

  @Test
  fun `rejects blank values`() {
    assertFailure { RoleLabel("") }.messageContains("must not be blank")
    assertFailure { RoleLabel("   ") }.messageContains("must not be blank")
  }

  @Test
  fun `rejects values longer than 64 characters`() {
    val tooLong = "x".repeat(RoleLabel.MaxLength + 1)
    assertFailure { RoleLabel(tooLong) }.messageContains("exceeds maximum length")
  }

  @Test
  fun `accepts values of exactly 64 characters`() {
    val maxLength = "x".repeat(RoleLabel.MaxLength)
    assertThat(RoleLabel(maxLength).value).isEqualTo(maxLength)
  }

  @Test
  fun `toString returns the raw value`() {
    assertThat(RoleLabel("Buchhalter Pro").toString()).isEqualTo("Buchhalter Pro")
  }
}
