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

import assertk.assertThat
import assertk.assertions.isLessThan
import assertk.assertions.isLessThanOrEqualTo
import org.junit.jupiter.api.Test
import java.lang.management.ManagementFactory
import kotlin.time.measureTime

/**
 * Budgets of the calendar on the JVM, measured as the best of [MeasuredRounds] after [WarmupRounds].
 */
class CalendarPerformanceTest {
  private val threadBean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean

  /** Keeps the JIT from dropping the measured work. */
  private var sink: Long = 0

  private val epochDays: LongArray = LongArray(Operations) { index ->
    LocalDate.MinEpochDay + (LocalDate.MaxEpochDay - LocalDate.MinEpochDay) / Operations * index
  }

  @Test
  fun `a million epoch day round trips take less than 100 ms and allocate the date only`() {
    val measurement = measure {
      epochDays.forEach { epochDay -> sink += LocalDate.ofEpochDay(epochDay).toEpochDay() }
    }
    assertThat(measurement.millis).isLessThan(100.0)
    // LocalDate: 12 bytes header, Long year, Int month, Int day of month.
    assertThat(measurement.bytesPerOperation).isLessThanOrEqualTo(32.0)
  }

  @Test
  fun `a million LocalDateTime fromMillis take less than 200 ms`() {
    val measurement = measure {
      epochDays.forEach { epochDay -> sink += LocalDateTime.fromMillis(epochDay * TimeConstants.MillisPerDay + 45_296_789.0, offset = 3_600_000.0).millis }
    }
    assertThat(measurement.millis).isLessThan(200.0)
  }

  private fun measure(workload: () -> Unit): Measurement {
    repeat(WarmupRounds) { workload() }
    val threadId = Thread.currentThread().threadId()
    var bestNanos = Long.MAX_VALUE
    var bestBytes = Long.MAX_VALUE
    repeat(MeasuredRounds) {
      val bytesBefore = threadBean.getThreadAllocatedBytes(threadId)
      val nanos = measureTime(workload).inWholeNanoseconds
      val bytes = threadBean.getThreadAllocatedBytes(threadId) - bytesBefore
      bestNanos = minOf(bestNanos, nanos)
      bestBytes = minOf(bestBytes, bytes)
    }
    return Measurement(millis = bestNanos / 1_000_000.0, bytesPerOperation = bestBytes.toDouble() / Operations).also { println("$it, sink $sink") }
  }

  private data class Measurement(val millis: Double, val bytesPerOperation: Double)

  private companion object {
    const val Operations: Int = 1_000_000
    const val WarmupRounds: Int = 5
    const val MeasuredRounds: Int = 5
  }
}
