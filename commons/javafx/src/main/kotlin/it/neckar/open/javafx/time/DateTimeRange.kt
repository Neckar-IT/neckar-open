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
package it.neckar.open.javafx.time

import it.neckar.open.unit.other.pct
import it.neckar.open.unit.si.ms
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.chrono.ChronoLocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Objects

/**
 * Represents a range between two date times
 *
 */
data class DateTimeRange(
  val start: LocalDateTime,
  val end: LocalDateTime,
) {

  val startAsMillis: @ms Long
    get() = toMillis(start)

  val endAsMillis: @ms Long
    get() = toMillis(end)

  val duration: Duration
    get() = Duration.between(start, end)

  /**
   * Returns the time at the given percentage between start and end
   */
  fun calcluateTimeAt(percentage: @pct Double): LocalDateTime {
    val duration = duration
    val millis: @ms Double = duration.toMillis() * percentage
    return start.plus(millis.toLong(), ChronoUnit.MILLIS)
  }

  /**
   * Returns the time at the center
   */
  val center: LocalDateTime
    get() {
      val duration = duration
      return start.plus(duration.dividedBy(2))
    }

  /**
   * Returns a new DateTimeRange moved by the given deltas
   */
  operator fun plus(deltaMillis: @ms Long): DateTimeRange {
    val newStart = start.plus(deltaMillis, ChronoUnit.MILLIS)
    val newEnd = end.plus(deltaMillis, ChronoUnit.MILLIS)
    return DateTimeRange(newStart, newEnd)
  }

  /**
   * Creates a new date range with the same duration but the center at the given position
   */
  fun withCenterAt(center: LocalDateTime): DateTimeRange {
    val duration = duration
    val start = center.minus(duration.dividedBy(2))
    val plus = center.plus(duration.dividedBy(2))
    return DateTimeRange(start, plus)
  }

  override fun toString(): String {
    return "DateTimeRange{" +
      "start=" + start +
      ", end=" + end +
      '}'
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as DateTimeRange
    return start == that.start && end == that.end
  }

  override fun hashCode(): Int {
    return Objects.hash(start, end)
  }

  companion object {
    /**
     * Represents no time
     */
    @JvmStatic
    val NO_TIME: LocalDateTime = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())

    @JvmStatic
    val NONE: DateTimeRange = DateTimeRange(NO_TIME, NO_TIME)

    /**
     * The earliest supported date. All dates before are invalid
     */
    @JvmStatic
    val EARLIEST_DATE: LocalDateTime = LocalDateTime.of(1980, 1, 1, 0, 0, 0)
    fun toMillis(localDateTime: ChronoLocalDateTime<LocalDate>): @ms Long {
      return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    @JvmStatic
    fun create(start: LocalDateTime, duration: Duration): DateTimeRange {
      return DateTimeRange(start, start.plus(duration))
    }
  }
}
