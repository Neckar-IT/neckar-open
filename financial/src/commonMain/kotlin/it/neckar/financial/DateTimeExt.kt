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
package it.neckar.financial

import it.neckar.open.unit.si.ms
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant


/**
 * Returns the current local date time in the given time zone
 */
fun LocalDateTime.Companion.now(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
  val now: Instant = it.neckar.open.time.now()
  return now.toLocalDateTime(timeZone)
}

fun LocalDate.Companion.now(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
  return LocalDateTime.now(timeZone).date
}

fun LocalDateTime.Companion.fromMillis(millis: @ms Double, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
  val now: Instant = Instant.fromEpochMilliseconds(millis.toLong())
  return now.toLocalDateTime(timeZone)
}

fun LocalDate.Companion.fromMillis(millis: @ms Double, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
  return LocalDateTime.fromMillis(millis, timeZone).date
}

fun LocalDate.plusDays(days: Int): LocalDate {
  return this.plus(days, DateTimeUnit.DAY)
}
