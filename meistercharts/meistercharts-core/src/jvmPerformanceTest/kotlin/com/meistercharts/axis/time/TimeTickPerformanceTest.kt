/*
 * Copyright 2023 Neckar IT GmbH, Mössingen, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meistercharts.axis.time

import assertk.assertThat
import assertk.assertions.isLessThan
import com.meistercharts.time.TimeViewport
import it.neckar.datetime.minimal.DayOfMonth
import it.neckar.datetime.minimal.LocalDate
import it.neckar.datetime.minimal.Month
import it.neckar.datetime.minimal.TimeZone
import it.neckar.datetime.minimal.Year
import it.neckar.open.unit.si.ms
import org.junit.jupiter.api.Test
import kotlin.time.DurationUnit
import kotlin.time.measureTime

/**
 * The ticks of one frame of a 1,500 px window take less than 0.1 ms in each window of the label table of the time axis.
 */
class TimeTickPerformanceTest {
  /** Keeps the JIT from dropping the measured work. */
  private var sink: Double = 0.0

  private val windows: List<Pair<Double, Double>> = listOf(
    TimeViewport.BigBangMillis to millisOf(2026, 11, 20),
    millisOf(1950 - 74_000_000L, 1, 1) to millisOf(1950 - 58_000_000L, 1, 1),
    millisOf(-11_999, 1, 1) to millisOf(-7_999, 1, 1),
    millisOf(-99, 1, 1) to millisOf(1, 1, 1),
    millisOf(1700, 1, 1) to millisOf(1800, 1, 1),
    millisOf(1789, 7, 10) to millisOf(1789, 7, 20),
    millisOf(1789, 7, 14) + 8 * 3_600_000.0 to millisOf(1789, 7, 14) + 20 * 3_600_000.0,
    millisOf(1789, 7, 14) + 14 * 3_600_000.0 to millisOf(1789, 7, 14) + 14 * 3_600_000.0 + 1_000.0,
  )

  @Test
  fun `ticks of one frame take less than 0,1 ms in every window`() {
    windows.forEach { (start, end) ->
      repeat(Warmup) { sink += frame(start, end) }
      val duration = measureTime { repeat(Frames) { sink += frame(start, end) } }
      val millisPerFrame = duration.toDouble(DurationUnit.MILLISECONDS) / Frames
      println("window ${start}..$end: $millisPerFrame ms per frame, sink $sink")
      assertThat(millisPerFrame).isLessThan(0.1)
    }
  }

  private fun frame(start: @ms Double, end: @ms Double): Double {
    val distance = TimeTickDistance.forTicks(100.0 * (end - start) / 1_500.0).withYearNumbering(YearNumbering.Calendar)
    return distance.calculateTicks(start, end, TimeZone.Berlin).size.toDouble()
  }

  private fun millisOf(year: Long, month: Int, day: Int): @ms Double {
    return LocalDate(Year(year), Month(month), DayOfMonth(day)).toEpochDay() * 86_400_000.0
  }

  private fun millisOf(year: Int, month: Int, day: Int): @ms Double {
    return millisOf(year.toLong(), month, day)
  }

  private companion object {
    const val Warmup: Int = 2_000
    const val Frames: Int = 2_000
  }
}
