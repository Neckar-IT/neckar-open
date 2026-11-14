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

import it.neckar.open.file.FileCopyManager.copy
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 *
 *
 * Date: 25.08.2006<br></br>
 * Time: 17:07:01<br></br>
 *
 * GlobalTilesCache *         [Xore Systems](http://www.xore.de)
 */
class FileCopyTest {
  @TempDir
  private lateinit var directory: File
  private lateinit var sourceDirectory: File

  @BeforeEach
  fun setUp() {
    sourceDirectory = directory.resolve("source").apply { mkdirs() }
    File(sourceDirectory, "test.txt").writeBytes(byteArrayOf(60))
  }

  @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  @Test
  fun testCopySimple() {
    val dst = directory.resolve("target").apply { mkdirs() }
    copy(sourceDirectory, dst)
    assertTrue(dst.exists())
    assertEquals(1, dst.list().size.toLong())
    assertEquals("test.txt", dst.list()[0])
  }

  @Test
  fun `copyFile copies the full content of a large file byte-for-byte`() {
    // Larger than any single transferTo/buffer window, so a truncating copy would be caught.
    val payload = ByteArray(8 * 1024 * 1024) { (it % 256).toByte() }
    val source = File(sourceDirectory, "large.bin")
    source.writeBytes(payload)

    val target = File(directory.resolve("target").apply { mkdirs() }, "large-copy.bin")
    FileCopyManager.copyFile(source, target)

    assertEquals(source.length(), target.length())
    assertArrayEquals(payload, target.readBytes())
  }

  @Test
  fun `copyDirectory copies a nested tree`() {
    val nested = File(sourceDirectory, "sub/deeper")
    nested.mkdirs()
    File(nested, "leaf.txt").writeText("leaf content")

    val dst = File(directory.resolve("target").apply { mkdirs() }, "copied")
    copy(sourceDirectory, dst)

    assertEquals("leaf content", File(dst, "sub/deeper/leaf.txt").readText())
  }

  @Test
  fun `deleteForced removes a whole directory tree`() {
    val root = directory.resolve("tree").apply { mkdirs() }
    File(root, "a/b").mkdirs()
    File(root, "a/b/c.txt").writeText("x")

    FileCopyManager.deleteForced(root)

    assertFalse(root.exists())
  }

  @Test
  fun `deleteForced fails fast on a non-existing file`() {
    val missing = directory.resolve("does-not-exist")
    assertThrows(IllegalArgumentException::class.java) {
      FileCopyManager.deleteForced(missing)
    }
  }

}
