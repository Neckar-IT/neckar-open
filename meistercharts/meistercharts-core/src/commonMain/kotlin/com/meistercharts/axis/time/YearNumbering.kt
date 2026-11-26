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

import it.neckar.open.unit.time.a

/**
 * Which years carry a tick of [DistanceYears], as astronomical year values.
 */
sealed interface YearNumbering {
  /**
   * The tick at or before [year]; it may lie below `Year.MinValue`.
   */
  fun tickAtOrBefore(year: Long, distanceInYears: @a Long): Long

  /**
   * The tick after [tick]
   */
  fun nextTick(tick: Long, distanceInYears: @a Long): Long

  /**
   * The ordinal of the tick at or before [year]; neighbouring ticks differ by 1.
   */
  fun tickIndex(year: Long, distanceInYears: @a Long): Long

  /**
   * Every multiple of the distance in astronomical counting: year -100 is 101 BC.
   */
  data object Astronomical : YearNumbering {
    override fun tickAtOrBefore(year: Long, distanceInYears: @a Long): Long {
      return year.floorDiv(distanceInYears) * distanceInYears
    }

    override fun nextTick(tick: Long, distanceInYears: @a Long): Long {
      return tick + distanceInYears
    }

    override fun tickIndex(year: Long, distanceInYears: @a Long): Long {
      return year.floorDiv(distanceInYears)
    }
  }

  /**
   * Round numbers as a reader counts them: from a distance of [BeforePresentFromDistance] on in years
   * before 1950, below in historical years without year 0 (100 BC, 50 BC, 10 BC, AD 10).
   */
  data object Calendar : YearNumbering {
    /**
     * From this distance on a tick counts years before present; the historical year of a tick 10,000 years apart says little.
     */
    const val BeforePresentFromDistance: @a Long = 10_000L

    /**
     * "Before present" counts from 1950, as radiocarbon dating and the chronostratigraphic chart of the ICS do.
     */
    const val BeforePresentReferenceYear: Long = 1_950L

    override fun tickAtOrBefore(year: Long, distanceInYears: @a Long): Long {
      if (countsBeforePresent(distanceInYears)) {
        return BeforePresentReferenceYear + (year - BeforePresentReferenceYear).floorDiv(distanceInYears) * distanceInYears
      }
      if (year <= 0L) {
        // 1 - year is the historical year BC; a tick lies at its multiples.
        return 1 + (year - 1).floorDiv(distanceInYears) * distanceInYears
      }
      val tick = year.floorDiv(distanceInYears) * distanceInYears
      return if (tick == 0L) 1 - distanceInYears else tick
    }

    override fun nextTick(tick: Long, distanceInYears: @a Long): Long {
      val candidate = tick + distanceInYears
      val skipsYearZero = countsBeforePresent(distanceInYears).not() && tick <= 0L && candidate > 0L
      return if (skipsYearZero) distanceInYears else candidate
    }

    override fun tickIndex(year: Long, distanceInYears: @a Long): Long {
      return when {
        countsBeforePresent(distanceInYears) -> (year - BeforePresentReferenceYear).floorDiv(distanceInYears)
        year <= 0L -> (year - 1).floorDiv(distanceInYears)
        // The tick before the first tick AD lies BC: the years 1 to 999 belong to the tick 1000 BC.
        else -> year.floorDiv(distanceInYears) - 1
      }
    }

    /**
     * Whether a tick of [distanceInYears] counts years before present instead of historical years.
     */
    fun countsBeforePresent(distanceInYears: @a Long): Boolean {
      return distanceInYears >= BeforePresentFromDistance
    }
  }
}
