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
package it.neckar.open.container

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.hasMessage
import assertk.assertions.isInstanceOf
import it.neckar.open.version.Version
import kotlin.test.Test

class VersionedEntryNameTest {
  @Test
  fun takesTheLastSegmentAsVersion() {
    assertThat(versionedName("config/users/1.2.0")).isEqualTo(VersionedEntryName(EntryName("config/users"), Version.of(1, 2, 0)))
  }

  @Test
  fun rejectsANameLeavingTheContainer() {
    assertFailure { versionedName("../x/1.0.0") }.isInstanceOf<IllegalArgumentException>()
    assertFailure { versionedName("/x/1.0.0") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun rejectsAZipPathAboveTheLimit() {
    assertFailure { versionedName("${"x".repeat(70_000)}/1.0.0") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun rejectsANameWithALoneSurrogate() {
    assertFailure { EntryName("mongodb\uD800") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun rejectsANameWithAControlCharacter() {
    assertFailure { EntryName("config\tusers") }.isInstanceOf<IllegalArgumentException>().hasMessage("Entry name <config\\u0009users> contains a control character")
  }

  @Test
  fun rejectsAVersionWithSuffix() {
    assertFailure { versionedName("mongodb/1.0.0-beta") }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun rejectsAVersionThatDoesNotFormatBackToItself() {
    assertFailure { versionedName("mongodb/01.0.0") }.isInstanceOf<IllegalArgumentException>()
  }
}
