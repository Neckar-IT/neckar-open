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
package it.neckar.common.featureflags

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test


class FeatureFlagsTest {
  @Test
  fun testToString() {
    val flags = FeatureFlags(FeatureFlag.slowUi, FeatureFlag("another"))

    val string = flags.encodeToString()
    assertThat(string).isEqualTo("slowUI,another")
    assertThat(FeatureFlags.decodeFromString(string)).isEqualTo(flags)
  }

  @Test
  fun testEqualsNoDescription() {
    assertThat(FeatureFlag.slowUi).isEqualTo(FeatureFlag(FeatureFlag.slowUi.key))
  }

  @Test
  fun testEncodeUnknown() {
    FeatureFlags.decodeFromString(
      FeatureFlags(FeatureFlag("doesnotexist")).encodeToString()
    ).flags.let {
      assertThat(it).hasSize(1)
      assertThat(it.first().key.value).isEqualTo("doesnotexist")
    }
  }

  @Test
  fun additional() {
    val flags = FeatureFlags(FeatureFlag.slowUi, FeatureFlag("another"))
    val newFlags = flags.withAdditional(FeatureFlag.testing)

    assertThat(newFlags.encodeToString()).isEqualTo("slowUI,another,testing")
    assertThat(newFlags.contains(FeatureFlag.slowUi)).isTrue()
    assertThat(newFlags.contains(FeatureFlag("foo"))).isFalse()

    val duplicateFlags = newFlags.withAdditional(FeatureFlag.testing)
    assertThat(newFlags.encodeToString()).isEqualTo(duplicateFlags.encodeToString())
  }

  @Test
  fun remove() {
    val flags = FeatureFlags(FeatureFlag.slowUi, FeatureFlag.testing)

    assertThat(flags.encodeToString()).isEqualTo("slowUI,testing")
    assertThat(flags.withRemovedFlag(FeatureFlag.testing).encodeToString()).isEqualTo("slowUI")
  }
}


