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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * Represents an interval for the time/date axis
 *
 */
enum class TickInterval(
  val interval: ChronoUnit,
  val amount: Int,
) {
  DECADE(ChronoUnit.DECADES, 1),
  YEAR(ChronoUnit.YEARS, 1),
  MONTH_6(ChronoUnit.MONTHS, 6),
  MONTH_3(ChronoUnit.MONTHS, 3),
  MONTH_1(ChronoUnit.MONTHS, 1),
  WEEK(ChronoUnit.WEEKS, 1),
  DAY(ChronoUnit.DAYS, 1),
  HOUR_12(ChronoUnit.HOURS, 12),
  HOUR_6(ChronoUnit.HOURS, 6),
  HOUR_3(ChronoUnit.HOURS, 3),
  HOUR_1(ChronoUnit.HOURS, 1),
  MINUTE_15(ChronoUnit.MINUTES, 15),
  MINUTE_5(ChronoUnit.MINUTES, 5),
  MINUTE_1(ChronoUnit.MINUTES, 1),
  SECOND_15(ChronoUnit.SECONDS, 15),
  SECOND_5(ChronoUnit.SECONDS, 5),
  SECOND_1(ChronoUnit.SECONDS, 1),
  MILLISECOND(ChronoUnit.MILLIS, 1);

  /**
   * Formats the given date depending on the interval
   */
  fun format(date: LocalDateTime): String {
    val dateTimeFormat: DateTimeFormatter
    val calendar = LocalDateTime.of(date.toLocalDate(), date.toLocalTime())
    dateTimeFormat = if (interval == ChronoUnit.YEARS && calendar.monthValue == 1 && calendar.dayOfMonth == 1) {
      DateTimeFormatter.ofPattern("yyyy")
    } else if (interval == ChronoUnit.MONTHS && calendar.dayOfMonth == 1) {
      DateTimeFormatter.ofPattern("MMMM yy")
    } else {
      when (interval) {
        ChronoUnit.DAYS, ChronoUnit.WEEKS -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        ChronoUnit.HOURS, ChronoUnit.MINUTES -> DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        ChronoUnit.SECONDS -> DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        //Localized MEDIUM time drops the milliseconds, so millisecond ticks would render identically.
        ChronoUnit.MILLIS -> DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
      }
    }
    return dateTimeFormat.format(date)
  }
}
