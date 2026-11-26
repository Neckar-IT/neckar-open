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

import it.neckar.open.unit.si.ms
import kotlin.math.floor

/**
 * Represents a local date + time
 */
data class LocalDateTime(
  val date: LocalDate,
  val time: LocalTime,
) : Comparable<LocalDateTime> {

  inline val year: Year
    get() {
      return date.year
    }
  inline val month: Month
    get() {
      return date.month
    }

  inline val dayOfMonth: DayOfMonth
    get() {
      return date.dayOfMonth
    }

  inline val hour: Int
    get() {
      return time.hour
    }

  inline val minute: Int
    get() {
      return time.minute
    }

  inline val second: Int
    get() {
      return time.second
    }

  inline val millis: Int
    get() {
      return time.millis
    }

  override fun compareTo(other: LocalDateTime): Int {
    return when {
      this.date != other.date -> this.date.compareTo(other.date)
      this.time != other.time -> this.time.compareTo(other.time)
      else -> 0
    }
  }

  /**
   * Creates a new instance at the start of the current hour
   */
  fun atStartOfHour(): LocalDateTime {
    return LocalDateTime(date, time.startOfHour())
  }

  /**
   * Creates a new instance at the start of the provided hour
   */
  fun atStartOfHour(hour: Int): LocalDateTime {
    return LocalDateTime(date, time.startOfHour(hour))
  }

  fun format(): String {
    return "${date.format()}T${time.format()}"
  }

  override fun toString(): String {
    return format()
  }

  /**
   * The instant of this local date and time at the time zone [offset] (local minus UTC).
   */
  fun toMillis(offset: @ms Double): @ms Double {
    return date.toEpochDay() * TimeConstants.MillisPerDay + time.millisOfDay() - offset
  }

  companion object {
    /**
     * Creates a new instance
     */
    fun of(
      year: Int, month: Int, day: Int,
      hour: Int, minute: Int, second: Int, millis: Int = 0,
    ): LocalDateTime {
      return LocalDateTime(
        LocalDate(year, month, day),
        LocalTime(hour, minute, second, millis)
      )
    }

    /**
     * The date and time at [millis] shifted by the time zone [offset] (local minus UTC).
     */
    fun fromMillis(millis: @ms Double, offset: @ms Double): LocalDateTime {
      val localMillis = millis + offset
      LocalDate.requireMillisInRange(localMillis)
      val epochDay = LocalDate.epochDayAt(localMillis)
      // Beyond 292 million years from 1970 the remainder misses its day by the resolution of Double.
      val millisOfDay = floor(localMillis - epochDay * TimeConstants.MillisPerDay).toInt().coerceIn(0, LocalTime.MillisPerDayInt - 1)
      return LocalDateTime(LocalDate.ofEpochDay(epochDay), LocalTime.ofMillisOfDay(millisOfDay))
    }
  }
}

/**
 * The date and time at [millis] in [expectedTimeZone], with the offset from [timeZoneOffsetProvider].
 */
fun LocalDateTime.Companion.fromMillisCurrentTimeZone(millis: @ms Double, expectedTimeZone: TimeZone): LocalDateTime {
  return fromMillis(millis, timeZoneOffsetProvider.offset(millis, expectedTimeZone))
}

/**
 * The instant of this local date and time in [timeZone].
 *
 * Resolves a transition like `java.time.ZonedDateTime.of`: a time in the gap moves forward by the
 * length of the gap, a time in the overlap takes the earlier offset.
 */
fun LocalDateTime.toMillis(timeZone: TimeZone): @ms Double {
  val localMillis = toMillis(offset = 0.0)
  val offsetBefore = timeZoneOffsetProvider.offset(localMillis - TimeConstants.MillisPerDay, timeZone)
  val offsetAfter = timeZoneOffsetProvider.offset(localMillis + TimeConstants.MillisPerDay, timeZone)

  val candidateBefore = localMillis - offsetBefore
  if (timeZoneOffsetProvider.offset(candidateBefore, timeZone) == offsetBefore) {
    return candidateBefore
  }
  val candidateAfter = localMillis - offsetAfter
  if (timeZoneOffsetProvider.offset(candidateAfter, timeZone) == offsetAfter) {
    return candidateAfter
  }
  // Gap: neither offset maps back to this local time.
  return candidateBefore
}
