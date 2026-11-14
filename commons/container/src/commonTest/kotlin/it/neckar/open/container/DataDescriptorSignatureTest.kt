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

import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

/** Content whose CRC-32 equals the data descriptor signature `0x08074b50`, so the reader cannot tell the signature from the CRC-32 by value. */
class DataDescriptorSignatureTest {
  private val content: ByteArray = "dump".encodeToByteArray().let { it + crc32Suffix(crc32(it), ZipSignatures.DataDescriptorSignature.toLong()) }

  @Test
  fun theContentHasTheSignatureAsChecksum() {
    assertThat(listOf(crc32(content))).containsExactly(ZipSignatures.DataDescriptorSignature.toLong())
  }

  @Test
  fun readsTheDescriptorWithSignature() {
    val bytes = container { writeEntry(versionedName("mongodb/1.0.0")) { it.write(content) } }

    assertThat(readAll(bytes).map { it.first to it.second.toList() }).containsExactly("mongodb/1.0.0" to content.toList())
  }


  private companion object {
    val table: LongArray = LongArray(256) { index ->
      var value = index.toLong()
      repeat(8) { value = if (value and 1L != 0L) (value ushr 1) xor 0xEDB88320L else value ushr 1 }
      value
    }

    fun crc32(bytes: ByteArray): Long {
      var register = 0xFFFFFFFFL
      for (byte in bytes) register = (register ushr 8) xor table[((register xor byte.toLong()) and 0xFF).toInt()]
      return register xor 0xFFFFFFFFL
    }

    /** The 4 bytes that turn a content with checksum [currentCrc] into one with checksum [wantedCrc]. */
    fun crc32Suffix(currentCrc: Long, wantedCrc: Long): ByteArray {
      // Every table entry has a different top byte, so the last 4 steps of the register run backwards.
      var register = wantedCrc xor 0xFFFFFFFFL
      repeat(4) {
        val index = table.indexOfFirst { (it ushr 24) == (register ushr 24) }
        register = ((register xor table[index]) shl 8 or index.toLong()) and 0xFFFFFFFFL
      }
      val suffix = register xor (currentCrc xor 0xFFFFFFFFL)
      return ByteArray(4) { (suffix ushr (8 * it)).toByte() }
    }
  }
}
