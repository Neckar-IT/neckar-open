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
package it.neckar.open.file

import assertk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 *
 */
class BaseNameAwareFileNamesTest {
  private lateinit var baseNameAwareFileNames: BaseNameAwareFileNames

  @Test
  fun testDuplicate() {
    baseNameAwareFileNames.add(FileName("base1", ".", "txt"))
    assertFailure {
      baseNameAwareFileNames.add(FileName("base1", ".", "txt"))
    }
  }

  @Test
  fun testIt() {
    val baseNameAwareFileNames = BaseNameAwareFileNames()
    baseNameAwareFileNames.add(FileName("base1", ".", "txt"))
    baseNameAwareFileNames.add(FileName("base1", ".", "txt2"))
    baseNameAwareFileNames.add(FileName("base1", ".", "txt3"))
    baseNameAwareFileNames.add(FileName("base2", ".", "txt"))
    baseNameAwareFileNames.add(FileName("base3", ".", "txt"))
    val fileNamesForBase1 = baseNameAwareFileNames.getOrCreateFileNames(BaseName("base1"))
    assertNotNull(fileNamesForBase1)
    assertEquals(3, fileNamesForBase1.getFileNames().size.toLong())
  }

  @BeforeEach
  fun setUp() {
    baseNameAwareFileNames = BaseNameAwareFileNames()
  }
}
