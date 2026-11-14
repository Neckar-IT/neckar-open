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
package it.neckar.csv

import assertk.*
import assertk.assertions.*
import kotlinx.coroutines.test.*
import kotlin.test.Test

class ConvertCsvJsTest {
  /**
   * Reproduces the charset bug: the previous `csvByteArray.toString()` returned the JS object
   * identity (e.g. "[object Int8Array]") and ignored [charset] entirely. Decoding the same
   * Windows-1252 bytes must now yield the original umlaut text.
   */
  @Test
  fun decodesWindows1252Umlaut() = runTest {
    // "Müller;30" encoded as Windows-1252 — ü is a single 0xFC byte (not the two-byte UTF-8 sequence)
    val windows1252Bytes = byteArrayOf(
      0x4D, 0xFC.toByte(), 0x6C, 0x6C, 0x65, 0x72, // Müller
      0x3B,                                        // ;
      0x33, 0x30,                                  // 30
    )

    val rows = convertCsv(windows1252Bytes, "windows-1252")

    assertThat(rows).hasSize(1)
    assertThat(rows[0]).containsExactly("Müller", "30")
  }
}
