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

actual fun LocalDateTime.Companion.fromMillisCurrentTimeZone(millis: @ms Double, expectedTimeZone: TimeZone): LocalDateTime {
  val parts = localDateTimeParts(millis)
  return LocalDateTime(
    date = LocalDate(parts[0], parts[1], parts[2]),
    time = LocalTime(hour = parts[3], minute = parts[4], second = parts[5], millis = parts[6]),
  )
}

actual fun LocalDateTime.toMillis(timeZone: TimeZone): @ms Double {
  return jsDateTimeMillis(year.value, month.value - 1, dayOfMonth.value, hour, minute, second, millis)
}

private fun jsDateTimeMillis(year: Int, monthZeroBased: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, millis: Int): Double =
  js("new Date(year, monthZeroBased, dayOfMonth, hour, minute, second, millis).getTime()")
