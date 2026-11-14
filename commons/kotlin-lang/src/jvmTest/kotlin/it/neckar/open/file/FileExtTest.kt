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
import assertk.assertions.*
import it.neckar.open.http.toUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.net.URI
import java.net.URL

@EnabledOnOs(OS.LINUX)
class FileExtTest {
  @Test
  fun testIt() {
    val path = "~/tmp"
    val file = File(path)

    assertThat(file.replaceLeadingTilde().absolutePath).isEqualTo(System.getProperty("user.home") + "/tmp")
  }

  @Test
  fun testToFixedURI() {
    val file = File("/home/paul/test/äüö $%&§@€ ßµ.txt")
    assertThat(file.toUrl().value).isEqualTo("file:///home/paul/test/%C3%A4%C3%BC%C3%B6%20\$%25&%C2%A7@%E2%82%AC%20%C3%9F%C2%B5.txt")
  }

  @Test
  fun testUrlConversions() {
    val file = File("/fubar/test/fileTest.txt")
    assertThat(file.absolutePath).isEqualTo("/fubar/test/fileTest.txt")

    assertThat(file.absoluteFile.toURI().toString()).isEqualTo("file:/fubar/test/fileTest.txt")
    assertThat(file.absoluteFile.toURI().toURL().toString()).isEqualTo("file:/fubar/test/fileTest.txt")

    assertThat(URI("file:/fubar/test/fileTest.txt").toString()).isEqualTo("file:/fubar/test/fileTest.txt")
    assertThat(URI("file:///fubar/test/fileTest.txt").toString()).isEqualTo("file:///fubar/test/fileTest.txt")
    assertThat(URI.create("file:///fubar/test/fileTest.txt").toString()).isEqualTo("file:///fubar/test/fileTest.txt")

    assertThat(URL("file:/fubar/test/fileTest.txt").toString()).isEqualTo("file:/fubar/test/fileTest.txt")
    assertThat(URL("file:///fubar/test/fileTest.txt").toString()).isEqualTo("file:/fubar/test/fileTest.txt")
    assertThat(URL("file:///fubar/test/fileTest.txt").toString()).isEqualTo("file:/fubar/test/fileTest.txt")

    assertThat(file.toUrl().toString()).isEqualTo("file:///fubar/test/fileTest.txt")
  }
}



