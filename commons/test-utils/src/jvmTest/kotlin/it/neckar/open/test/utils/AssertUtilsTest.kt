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
import it.neckar.open.resources.getResourceSafe
import it.neckar.open.test.utils.AssertFileUtils.assertFileByHash
import it.neckar.open.test.utils.AssertFileUtils.createCopyFile
import it.neckar.open.test.utils.AssertFileUtils.createPath
import it.neckar.open.test.utils.AssertUtils.guessPathFromStackTrace
import it.neckar.open.test.utils.JsonUtils.assertJsonEquals
import it.neckar.open.test.utils.JsonUtils.formatJson
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opentest4j.AssertionFailedError
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertFailsWith

/**
 *
 */
class AssertUtilsTest {
  @Test
  @Throws(Exception::class)
  fun testJsonFormat() {
    val separator = System.getProperty("line.separator")

    assertThat(
      formatJson("{\"id\":\"asdfasdf\",   \"unformated\":true}")
    ).isEqualTo(
      "{" + separator +
        "  \"id\" : \"asdfasdf\"," + separator +
        "  \"unformated\" : true" + separator +
        "}"
    )

    assertThat(
      formatJson("{\"id\":\"asdfasdf\", \"unformated\":true}")
    ).isEqualTo(
      "{" + separator +
        "  \"id\" : \"asdfasdf\"," + separator +
        "  \"unformated\" : true" + separator +
        "}"
    )
  }

  @Test
  @Throws(Exception::class)
  fun testJsonEquals() {
    assertJsonEquals("{\"id\":\"asdfasdf\",   \"unformated\":true}", "{\"id\":\"asdfasdf\",   \"unformated\":true}")
    assertJsonEquals("{\"id\":\"asdfasdf\",   \"unformated\":true}", "{\"id\":\"asdfasdf\",\"unformated\":true}")
  }

  @Test
  @Throws(Exception::class)
  fun testJsonNotEquals() {
    assertFailsWith<AssertionFailedError> {
      assertJsonEquals("{\"id\":\"asdfasdf\",   \"unformated\":true}", "{\"id\":\"asdfasdf\",   \"unformated\":false}")
    }
  }

  @Test
  @Throws(Exception::class)
  fun testJsonNotFormatable() {
    assertFailsWith<AssertionFailedError> {
      assertJsonEquals(null as String?, null)
    }
    assertFailsWith<AssertionError> {
      assertJsonEquals("affase", "asdf")
    }.also { e ->
      assertThat(e.message!!).contains("JSON parsing error")

    }
  }

  @Test
  fun testAssertWithResource() {
    assertThat(
      """
   the content of the file...
   second line!
   third line!
   """.trimIndent()
    ).isEqualTo(javaClass.getResourceSafe("equals.txt").readText(StandardCharsets.UTF_8))


    assertFailsWith<AssertionError> {
      assertThat("other!").isEqualTo(javaClass.getResourceSafe("equals.txt"))
    }
  }

  @Test
  @Throws(Exception::class)
  fun testFileByHash(@TempDir directory: File) {
    val file = directory.resolve("daFile")
    FileUtils.writeStringToFile(file, "daContent", StandardCharsets.UTF_8)
    val expected = HashAlgorithm.MD5.calculate(file = file)
    assertThat(expected.valueAsHex).isEqualTo("913aa4a45cea16f9714f109e7324159f")
    assertFileByHash("path", expected, file)
    assertFileByHash("path", HashAlgorithm.MD5.calculate(file = file), file)
    assertFileByHash("path", HashAlgorithm.SHA256.calculate(file = file), file)
    assertFileByHash("path", HashAlgorithm.SHA1.calculate(file = file), file)
    assertFileByHash("path", HashAlgorithm.SHA512.calculate(file = file), file)
    try {
      assertFileByHash(AssertUtilsTest::class.java, "testFileByHash", fromHex(HashAlgorithm.MD5, "aa"), file)
      throw RuntimeException("expected assertion")
    } catch (e: AssertionError) {
      val copiedFile = createCopyFile(createPath(AssertUtilsTest::class.java, "testFileByHash"), "daFile")
      assertThat(e.message!!).contains(copiedFile.absolutePath)
      assertThat(copiedFile.parentFile.exists()).isTrue()
      assertThat(copiedFile.exists()).isTrue()
      assertThat(FileUtils.readFileToString(copiedFile, StandardCharsets.UTF_8)).isEqualTo("daContent")
    }
    try {
      assertFileByHash(fromHex(HashAlgorithm.MD5, "aa"), file)
      throw RuntimeException("expected assertion")
    } catch (e: AssertionError) {
      assertThat(e.message!!).contains("Stored questionable file under test at")
    }
  }

  @Test
  @Throws(Exception::class)
  fun testGuessPath() {
    assertThat(guess()).isEqualTo(AssertUtilsTest::class.java.name + File.separator + "testGuessPath")
    assertThat(guess()).isEqualTo("it.neckar.open.test.utils.AssertUtilsTest" + File.separator + "testGuessPath")
  }

  @Test
  @Throws(Exception::class)
  fun testGuessPath2() {
    assertThat(guess()).isEqualTo(AssertUtilsTest::class.java.name + File.separator + "testGuessPath2")
  }

  //this method is necessary to simulate the call to AssertUtils.assertFileByHash
  private fun guess(): String {
    return guessPathFromStackTrace()
  }
}
