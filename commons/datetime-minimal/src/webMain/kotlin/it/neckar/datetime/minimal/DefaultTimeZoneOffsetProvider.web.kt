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
 * `Temporal` where the runtime has it, else the `longOffset` name of `Intl.DateTimeFormat`; both read
 * the IANA time zone database of the runtime.
 */
internal actual fun platformTimeZoneOffset(timestamp: @ms Double, timeZone: TimeZone): @ms Double {
  val millis = floor(timestamp)
  val temporalOffset = temporalOffsetMillisOrNaN(millis, timeZone.zoneId)
  if (temporalOffset.isNaN().not()) {
    return temporalOffset
  }
  return parseGmtOffset(intlLongOffsetName(millis, timeZone.zoneId))
}

private fun temporalOffsetMillisOrNaN(millis: Double, zoneId: String): Double = js(
  "(globalThis.Temporal ? globalThis.Temporal.Instant.fromEpochMilliseconds(millis).toZonedDateTimeISO(zoneId).offsetNanoseconds / 1e6 : NaN)"
)

private fun intlLongOffsetName(millis: Double, zoneId: String): String = js(
  "(new Intl.DateTimeFormat('en-US', { timeZone: zoneId, timeZoneName: 'longOffset' })).formatToParts(new Date(millis)).find(function (part) { return part.type === 'timeZoneName'; }).value"
)
