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
package it.neckar.open.test.utils.matchers

import assertk.*
import assertk.assertions.*
import com.google.common.io.Files
import it.neckar.open.test.utils.containsFiles
import it.neckar.open.test.utils.isEmptyDirectory
import it.neckar.open.test.utils.matchers.ContainsFileMatcher.Companion.toTree
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

/**
 *
 */
class ContainsFileMatcherTest {

  @TempDir
  private lateinit var directory: File

  @Test
  fun testBasic() {
    Files.touch(directory.resolve("a"))
    assertThat(directory).containsFiles("a")
    Files.touch(directory.resolve("b"))
    assertThat(directory).containsFiles("a", "b")
  }

  @Test
  fun testSubDir() {
    Files.touch(File(directory.resolve("dir").apply { mkdirs() }, "a"))
    assertThat(directory).containsFiles("dir/a")
  }

  @Test
  fun testEmpty() {
    assertThat(directory).isEmptyDirectory()
    assertThrows(AssertionError::class.java) {
      Files.touch(directory.resolve("a"))
      assertThat(directory).isEmptyDirectory()
    }
  }

  @Test
  fun testList() {
    run {
      val dir = directory.resolve("dir").apply { mkdirs() }
      Files.touch(File(dir, "b"))
      Files.touch(File(dir, "a"))
      Files.touch(File(dir, "c"))
    }
    run {
      val dir = directory.resolve("dir2").apply { mkdirs() }
      Files.touch(File(dir, "a"))
      Files.touch(File(dir, "c"))
      Files.touch(File(dir, "b"))
    }
    val tree = toTree(directory)
    assertTrue(tree.contains("dir" + File.separator + "a"))
    assertTrue(tree.contains("dir" + File.separator + "b"))
    assertTrue(tree.contains("dir" + File.separator + "c"))
    assertTrue(tree.contains("dir2" + File.separator + "a"))
    assertTrue(tree.contains("dir2" + File.separator + "b"))
    assertTrue(tree.contains("dir2" + File.separator + "c"))
  }

  @Test
  @Throws(IOException::class)
  fun testNontExistent() {
    try {
      assertThat(directory).containsFiles("a")
      throw RuntimeException("expected assertion")
    } catch (ignore: AssertionError) {
    }
  }
}
