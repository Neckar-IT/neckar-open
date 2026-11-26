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

/**
 * The range of `Temporal.PlainDate`: -271821-04-19 to 275760-09-13.
 */
private val temporalDateRange: ClosedRange<LocalDate> = LocalDate(-271821, 4, 19)..LocalDate(275760, 9, 13)

fun LocalDate.toTemporal(): Temporal.PlainDate {
  requireInTemporalRange(this)
  return Temporal.PlainDate(year.toInt(), month.value, dayOfMonth.value)
}

fun LocalDate.Companion.fromTemporal(date: Temporal.PlainDate): LocalDate {
  return LocalDate(date.year, date.month, date.day)
}

fun LocalDateTime.toTemporal(): Temporal.PlainDateTime {
  requireInTemporalRange(date)
  return Temporal.PlainDateTime(year.toInt(), month.value, dayOfMonth.value, hour, minute, second, millis)
}

fun LocalDateTime.Companion.fromTemporal(dateTime: Temporal.PlainDateTime): LocalDateTime {
  return LocalDateTime(
    LocalDate(dateTime.year, dateTime.month, dateTime.day),
    LocalTime(dateTime.hour, dateTime.minute, dateTime.second, dateTime.millisecond),
  )
}

private fun requireInTemporalRange(date: LocalDate) {
  require(date in temporalDateRange) {
    "Date $date outside the range of Temporal ${temporalDateRange.start}..${temporalDateRange.endInclusive}"
  }
}
