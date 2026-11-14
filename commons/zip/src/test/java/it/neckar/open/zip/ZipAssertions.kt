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

import org.junit.jupiter.api.Assertions
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.annotation.Nonnull

import assertk.*
import assertk.assertions.*

/**
 * Assertions for zip files
 */
object ZipAssertions {
  fun compare(@Nonnull expected: ZipFile, @Nonnull actual: ZipFile) {
    if (expected === actual) {
      return
    }
    val testedActualEntries: MutableSet<String> = HashSet(actual.size())

    //Forward
    val expectedEntries = expected.entries()
    while (expectedEntries.hasMoreElements()) {
      val expectedEntry = expectedEntries.nextElement()
      val actualEntry = actual.getEntry(expectedEntry.name)
      compare(expectedEntry, actualEntry)
      testedActualEntries.add(actualEntry.name)
    }

    //Backward
    val actualEntries = actual.entries()
    while (actualEntries.hasMoreElements()) {
      val actualEntry = actualEntries.nextElement()
      if (testedActualEntries.contains(actualEntry.name)) {
        continue
      }
      throw AssertionError("Uups. Unexpected element in actual: " + actualEntry.name)
    }
    Assertions.assertEquals(expected.size(), actual.size())
  }

  fun compare(expectedEntry: ZipEntry?, actualEntry: ZipEntry?) {
    if (expectedEntry === actualEntry) {
      return
    }
    if (expectedEntry == null) {
      throw AssertionError("ExpectedEntry is null for " + actualEntry!!.name)
    }
    if (actualEntry == null) {
      throw AssertionError("ActualEntry is null for " + expectedEntry.name)
    }
    Assertions.assertEquals(expectedEntry.name, actualEntry.name)
    Assertions.assertEquals(expectedEntry.crc, actualEntry.crc)
    Assertions.assertEquals(expectedEntry.size, actualEntry.size)
    Assertions.assertEquals("Different Name. Expected: <" + expectedEntry.name + "> but was <" + actualEntry.name + ">", expectedEntry.name, actualEntry.name)
    assertThat(actualEntry.size).isEqualTo(expectedEntry.size)
    assertThat(actualEntry.crc).isEqualTo(expectedEntry.crc)
  }
}
