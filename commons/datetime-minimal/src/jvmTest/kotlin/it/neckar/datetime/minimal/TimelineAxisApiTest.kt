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
package it.neckar.datetime.minimal

import assertk.*
import assertk.assertions.*
import it.neckar.open.time.formatUtcForDebug
import it.neckar.open.unit.si.ms
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.util.DefaultTimeZone

class TimelineAxisApiTest {
  val time: @ms Double = 1.546855919123123E12

  /**
   * tests the API usage as the time axis will do
   */
  @DefaultTimeZone("Europe/Berlin")
  @Test
  fun testIt() {
    val leftSideOfWindowTime = time
    val rightSideOfWindowTime = time + 10_934_400_017.0

    assertThat(leftSideOfWindowTime.formatUtcForDebug()).isEqualTo("2019-01-07T10:11:59.123Z")
    assertThat(rightSideOfWindowTime.formatUtcForDebug()).isEqualTo("2019-05-13T23:31:59.140Z")

    //find the full month before the left side of the window
    val leftSideLocalDate = LocalDate.fromMillisCurrentTimeZone(leftSideOfWindowTime, TimeZone.Berlin)
    assertThat(leftSideLocalDate.format()).isEqualTo("2019-01-07")

    val rightSideLocalDate = LocalDate.fromMillisCurrentTimeZone(rightSideOfWindowTime, TimeZone.Berlin)
    assertThat(rightSideLocalDate.format()).isEqualTo("2019-05-14")

    val rightSideStartOfMonth = rightSideLocalDate.atStartOfMonth()
    assertThat(rightSideStartOfMonth.format()).isEqualTo("2019-05-01")

    val rightSideNextMonth = rightSideStartOfMonth.plusMonths(1)
    assertThat(rightSideNextMonth.format()).isEqualTo("2019-06-01")


    //Calculate ticks
    val ticks = mutableListOf<LocalDate>()

    var current = leftSideLocalDate.atStartOfMonth()
    while (current <= rightSideNextMonth) {
      ticks.add(current)
      current = current.plusMonths(1)
    }

    assertThat(ticks).hasSize(6)
  }
}
