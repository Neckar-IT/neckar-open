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
package it.neckar.open.test.utils

import assertk.*
import assertk.assertions.*
import it.neckar.open.crypt.Hash.Companion.fromHex
import it.neckar.open.crypt.HashAlgorithm
import it.neckar.open.test.utils.AssertFileUtils.assertFileByHash
import it.neckar.open.test.utils.AssertFileUtils.assertFileByHashes
import it.neckar.open.test.utils.AssertFileUtils.createCopyFile
import it.neckar.open.test.utils.AssertUtils.FailedFilesDir
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertFailsWith

/**
 *
 */
class AssertUtilsFileCompareTest {
  private var fileA: File? = null
  private var fileB: File? = null

  @BeforeEach
  fun setUp(@TempDir directory: File) {
    fileA = directory.resolve("a")
    FileUtils.writeStringToFile(fileA, "daContent", StandardCharsets.UTF_8)
    fileB = directory.resolve("b")
    FileUtils.writeStringToFile(fileB, "daContentB", StandardCharsets.UTF_8)
  }

  @Test
  fun testCopyFile() {
    val dir = FailedFilesDir
    assertEquals(File(dir, "daPath" + File.separator + "daName"), createCopyFile("daPath", "daName"))
    assertEquals(File(dir, "daPath2" + File.separator + "other" + File.separator + "daName2"), createCopyFile("daPath2" + File.separator + "other", "daName2"))
  }

  @Test
  fun testBasic() {
    assertFileByHash(fromHex(HashAlgorithm.MD5, "913aa4a45cea16f9714f109e7324159f"), fileA!!)
  }

  @Test
  fun testInvalid() {
    assertFailsWith<IllegalArgumentException> {
      assertFileByHashes(fileA!!)
    }
  }

  @Test
  fun testOneOf() {
    assertFileByHashes(
      fileA!!, fromHex(HashAlgorithm.MD5, "913aa4a45cea16f9714f109e7324159f"), fromHex(HashAlgorithm.MD5, "AA"), fromHex(HashAlgorithm.MD5, "BB")
    )
  }

  @Test
  fun testOneOf2() {
    assertFileByHashes(
      fileA!!, fromHex(HashAlgorithm.MD5, "AA"), fromHex(HashAlgorithm.MD5, "913aa4a45cea16f9714f109e7324159f"), fromHex(HashAlgorithm.MD5, "BB")
    )
  }

  @Test
  fun testOneOf4() {
    assertFileByHashes(
      fileA!!, HashAlgorithm.MD5, "AA", "913aa4a45cea16f9714f109e7324159f", "BB"
    )
  }

  @Test
  fun testOneOfFail() {
    try {
      assertFileByHashes(
        fileA!!, fromHex(HashAlgorithm.MD5, "AA"), fromHex(HashAlgorithm.MD5, "CC"), fromHex(HashAlgorithm.MD5, "BB")
      )
      throw RuntimeException("expected assertion")
    } catch (e: AssertionError) {
      assertThat(e.message!!).contains("Stored questionable file under test")
    }
  }

  @Test
  fun testFail() {
    try {
      assertFileByHash(fromHex(HashAlgorithm.MD5, "AA"), fileA!!)
      throw RuntimeException("expected assertion")
    } catch (e: AssertionError) {
      assertThat(e.message!!).contains("Stored questionable file under test")
    }
  }
}
