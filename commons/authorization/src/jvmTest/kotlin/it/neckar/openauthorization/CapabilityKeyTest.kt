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
import it.neckar.open.authorization.CapabilityKey
import org.junit.jupiter.api.Test

class CapabilityKeyTest {
  @Test
  fun `accepts well-formed namespace and action`() {
    assertThat(CapabilityKey("core:read-data").value).isEqualTo("core:read-data")
    assertThat(CapabilityKey("patterns:manage-books").value).isEqualTo("patterns:manage-books")
    assertThat(CapabilityKey("a:b").value).isEqualTo("a:b")
  }

  @Test
  fun `accepts digits and hyphens after the leading letter`() {
    assertThat(CapabilityKey("ns1:action-2").value).isEqualTo("ns1:action-2")
  }

  @Test
  fun `rejects missing colon`() {
    assertFailure { CapabilityKey("coreread") }.messageContains("does not match")
  }

  @Test
  fun `rejects empty namespace`() {
    assertFailure { CapabilityKey(":read") }.messageContains("does not match")
  }

  @Test
  fun `rejects empty action`() {
    assertFailure { CapabilityKey("core:") }.messageContains("does not match")
  }

  @Test
  fun `rejects uppercase letters`() {
    assertFailure { CapabilityKey("Core:read") }.messageContains("does not match")
    assertFailure { CapabilityKey("core:Read") }.messageContains("does not match")
  }

  @Test
  fun `rejects leading digit`() {
    assertFailure { CapabilityKey("1ns:read") }.messageContains("does not match")
    assertFailure { CapabilityKey("ns:1read") }.messageContains("does not match")
  }

  @Test
  fun `rejects underscore and other special characters`() {
    assertFailure { CapabilityKey("core:read_data") }.messageContains("does not match")
    assertFailure { CapabilityKey("core:read data") }.messageContains("does not match")
  }

  @Test
  fun `toString returns the raw value`() {
    assertThat(CapabilityKey("core:read-data").toString()).isEqualTo("core:read-data")
  }
}
