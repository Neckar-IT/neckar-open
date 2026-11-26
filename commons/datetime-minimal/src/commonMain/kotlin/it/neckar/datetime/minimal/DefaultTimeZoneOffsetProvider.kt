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

import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.unit.si.ms

/**
 * Reads the offset from the time zone database of the platform.
 *
 * The platform databases cover ±8.64e15 ms around 1970; outside, the offset at the nearest bound applies.
 */
class DefaultTimeZoneOffsetProvider : TimeZoneOffsetProvider {
  override fun offset(timestamp: @ms Double, timeZone: TimeZone): @ms Double {
    return platformTimeZoneOffset(timestamp.coerceIn(-MaxPlatformMillis, MaxPlatformMillis), timeZone)
  }

  companion object {
    /**
     * The range of `Date` and `Temporal.Instant`; the JVM takes the same bound, so every platform returns the same offset.
     */
    const val MaxPlatformMillis: @ms Double = 8.64e15
  }
}

/**
 * The offset (local minus UTC) of [timeZone] at [timestamp], which lies within ±[DefaultTimeZoneOffsetProvider.MaxPlatformMillis].
 */
internal expect fun platformTimeZoneOffset(timestamp: @ms Double, timeZone: TimeZone): @ms Double

/**
 * Parses the `longOffset` name of `Intl.DateTimeFormat`: `GMT`, `GMT+01:00`, `GMT+00:53:28`, `GMT-03:30`.
 */
internal fun parseGmtOffset(gmtOffsetName: String): @ms Double {
  val match = gmtOffsetPattern.matchEntire(gmtOffsetName).requireNotNull {
    "Invalid GMT offset <$gmtOffsetName>"
  }
  val (sign, hours, minutes, seconds) = match.destructured
  if (sign.isEmpty()) {
    return 0.0
  }
  val absoluteSeconds = hours.toInt() * 3600 + minutes.toInt() * 60 + (seconds.toIntOrNull() ?: 0)
  val signFactor = if (sign == "+") 1 else -1
  return signFactor * absoluteSeconds * 1000.0
}

// Some locales write the minus as U+2212.
private val gmtOffsetPattern: Regex = Regex("""GMT(?:([+\-−])(\d{2}):(\d{2})(?::(\d{2}))?)?""")
