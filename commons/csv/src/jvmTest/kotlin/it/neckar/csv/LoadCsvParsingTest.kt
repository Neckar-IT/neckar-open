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
import org.junit.jupiter.api.Test

/**
 * Verifies how [convertCsv] parses the bytes it is given - delimiter, quoting, line breaks and charset.
 */
class LoadCsvParsingTest {
  @Test
  fun splitsRowsAtSemicolons() {
    runTest {
      val rows = convertCsv("1950;Januar;49505\n1950;Februar;47177".encodeToByteArray(), "UTF-8")

      assertThat(rows).containsExactly(
        listOf("1950", "Januar", "49505"),
        listOf("1950", "Februar", "47177"),
      )
    }
  }

  @Test
  fun doesNotSplitAtCommas() {
    runTest {
      val rows = convertCsv("1950,Januar,49505".encodeToByteArray(), "UTF-8")

      assertThat(rows).containsExactly(listOf("1950,Januar,49505"))
    }
  }

  @Test
  fun keepsSemicolonAndLineBreakInsideQuotedField() {
    runTest {
      val rows = convertCsv("\"Mössingen; Neckar\";\"first\nsecond\";42".encodeToByteArray(), "UTF-8")

      assertThat(rows).containsExactly(listOf("Mössingen; Neckar", "first\nsecond", "42"))
    }
  }

  @Test
  fun keepsEmptyFields() {
    runTest {
      val rows = convertCsv("1950;;;46331".encodeToByteArray(), "UTF-8")

      assertThat(rows).containsExactly(listOf("1950", "", "", "46331"))
    }
  }

  @Test
  fun handlesWindowsLineEndings() {
    runTest {
      val rows = convertCsv("1950;Januar\r\n1950;Februar\r\n".encodeToByteArray(), "UTF-8")

      assertThat(rows).containsExactly(
        listOf("1950", "Januar"),
        listOf("1950", "Februar"),
      )
    }
  }

  @Test
  fun decodesWindows1252Umlaut() {
    runTest {
      // "März;53604" encoded as Windows-1252 - ä is a single 0xE4 byte (not the two byte UTF-8 sequence)
      val windows1252Bytes = byteArrayOf(
        0x4D, 0xE4.toByte(), 0x72, 0x7A, // März
        0x3B,                            // ;
        0x35, 0x33, 0x36, 0x30, 0x34,    // 53604
      )

      assertThat(convertCsv(windows1252Bytes, "windows-1252")).containsExactly(listOf("März", "53604"))
    }
  }

  @Test
  fun returnsNoRowsForEmptyInput() {
    runTest {
      assertThat(convertCsv(ByteArray(0), "UTF-8")).isEmpty()
    }
  }
}
