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

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import it.neckar.open.authorization.Capability
import it.neckar.open.authorization.CapabilityKey
import it.neckar.open.authorization.CoreCapability
import it.neckar.open.authorization.UserCapabilities
import org.junit.jupiter.api.Test

class UserCapabilitiesTest {
  @Test
  fun `contains returns true when capability key is present`() {
    val capabilities = UserCapabilities(
      setOf(
        CoreCapability.ReadData.key,
        CoreCapability.WriteData.key,
      ),
    )

    assertThat(CoreCapability.ReadData in capabilities).isTrue()
    assertThat(CoreCapability.WriteData in capabilities).isTrue()
  }

  @Test
  fun `contains returns false when capability key is absent`() {
    val capabilities = UserCapabilities(setOf(CoreCapability.ReadData.key))

    assertThat(CoreCapability.WriteData in capabilities).isFalse()
    assertThat(CoreCapability.ManageMembers in capabilities).isFalse()
  }

  @Test
  fun `containsAll returns true when every capability is present`() {
    val capabilities = UserCapabilities(
      setOf(
        CoreCapability.ReadData.key,
        CoreCapability.WriteData.key,
        CoreCapability.ManageMembers.key,
      ),
    )

    assertThat(
      capabilities.containsAll(
        listOf(CoreCapability.ReadData, CoreCapability.WriteData),
      ),
    ).isTrue()
  }

  @Test
  fun `containsAll returns false when any capability is missing`() {
    val capabilities = UserCapabilities(
      setOf(
        CoreCapability.ReadData.key,
        CoreCapability.WriteData.key,
      ),
    )

    assertThat(
      capabilities.containsAll(
        listOf(CoreCapability.ReadData, CoreCapability.ManageTenant),
      ),
    ).isFalse()
  }

  @Test
  fun `empty user has no capabilities`() {
    assertThat(CoreCapability.ReadData in UserCapabilities.Empty).isFalse()
    assertThat(
      UserCapabilities.Empty.containsAll(listOf(CoreCapability.ReadData)),
    ).isFalse()
  }

  @Test
  fun `containsAll on empty collection always passes`() {
    assertThat(UserCapabilities.Empty.containsAll(emptyList())).isTrue()
  }

  @Test
  fun `product-defined capability participates in checks via its key`() {
    val custom = object : Capability {
      override val key: CapabilityKey = CapabilityKey("patterns:manage-books")
    }
    val capabilities = UserCapabilities(setOf(custom.key))

    assertThat(custom in capabilities).isTrue()
  }
}
