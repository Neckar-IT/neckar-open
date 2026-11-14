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
package it.neckar.open.zip

import assertk.*
import assertk.assertions.*
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class ZipExtractorTest {
  @Test
  fun `extracts a benign entry into the destination`(@TempDir destination: File) {
    val zipBytes = buildZip("hello.txt" to "hi".toByteArray())
    ZipExtractor().extract(destination, ByteArrayInputStream(zipBytes))

    val extracted = File(destination, "hello.txt")
    assertThat(extracted.exists()).isTrue()
    assertThat(extracted.readText()).isEqualTo("hi")
  }

  @Test
  fun `rejects an entry whose name escapes the destination (Zip Slip)`(@TempDir root: File) {
    val destination = root.resolve("destination").apply { mkdirs() }
    // Standard "Zip Slip" payload — the entry name encodes a parent-directory hop.
    val zipBytes = buildZip("../escape.txt" to "owned".toByteArray())

    assertFailure {
      ZipExtractor().extract(destination, ByteArrayInputStream(zipBytes))
    }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("resolves outside destination")

    // Crucially: nothing was written outside the destination.
    val parentEscape = File(root, "escape.txt")
    assertThat(parentEscape.exists()).isFalse()
  }

  @Test
  fun `creates missing parent directories for nested file entries`(@TempDir destination: File) {
    //buildZip emits only file entries (no explicit directory entries) - like many zip writers.
    val zipBytes = buildZip("sub/dir/nested.txt" to "deep".toByteArray())
    ZipExtractor().extract(destination, ByteArrayInputStream(zipBytes))

    val extracted = File(destination, "sub/dir/nested.txt")
    assertThat(extracted.exists()).isTrue()
    assertThat(extracted.readText()).isEqualTo("deep")
  }

  private fun buildZip(vararg entries: Pair<String, ByteArray>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipArchiveOutputStream(out).use { zos ->
      for ((name, payload) in entries) {
        val entry = ZipArchiveEntry(name)
        entry.size = payload.size.toLong()
        zos.putArchiveEntry(entry)
        zos.write(payload)
        zos.closeArchiveEntry()
      }
    }
    return out.toByteArray()
  }
}
