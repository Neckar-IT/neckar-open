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
@file:Suppress("RETURN_VALUE_NOT_USED_COERCION") // assertFailure takes a () -> Unit, so the getter under test cannot hand its value anywhere.

package it.neckar.open.file

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.util.ClearSystemProperty
import org.junit.jupiter.api.util.RestoreSystemProperties
import org.junit.jupiter.api.util.SetSystemProperty
import java.nio.file.Files
import java.nio.file.Path

class TempDirectoryTest {

  @Test
  fun `is the absolute directory the property names`() {
    assertThat(tempDirectory.isAbsolute).isTrue()
    assertThat(Files.isDirectory(tempDirectory)).isTrue()
    assertThat(tempDirectory.toString()).isEqualTo(System.getProperty(TempDirectoryProperty))
  }

  @Test
  @RestoreSystemProperties
  fun `follows the property when it moves`(@TempDir moved: Path) {
    System.setProperty(TempDirectoryProperty, moved.toString())

    assertThat(tempDirectory).isEqualTo(moved)
  }

  @Test
  @SetSystemProperty(key = TempDirectoryProperty, value = "")
  fun `refuses an empty path`() {
    assertFailure { tempDirectory }
      .isInstanceOf(IllegalStateException::class)
      .messageContains("temporary files would land in the working directory")
  }

  @Test
  @SetSystemProperty(key = TempDirectoryProperty, value = "build/tmp")
  fun `refuses a relative path`() {
    assertFailure { tempDirectory }
      .isInstanceOf(IllegalStateException::class)
      .messageContains("build/tmp")
  }

  @Test
  @ClearSystemProperty(key = TempDirectoryProperty)
  fun `names the property when it is unset`() {
    assertFailure { tempDirectory }
      .isInstanceOf(IllegalStateException::class)
      .messageContains(TempDirectoryProperty)
  }
}
