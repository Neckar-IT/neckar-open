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

import it.neckar.open.collections.Cache
import it.neckar.open.collections.cache
import it.neckar.open.kotlin.lang.floor
import it.neckar.open.unit.si.ms
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

/**
 * A time zone offset provider that returns cached values computed by its [delegate].
 */
class DefaultCachedTimeZoneOffsetProvider(
  val delegate: TimeZoneOffsetProvider,
  /**
   * The maximum size of the cache
   */
  cacheSize: Int = 500,
) : CachedTimeZoneOffsetProvider {

  init {
    require(delegate !is CachedTimeZoneOffsetProvider) { "cannot cache an already cached time zone offset provider" }
  }

  private val cache: Cache<Int, @ms Double> = cache("Cache for CachedTimeZoneOffsetProvider", cacheSize)

  override val currentCacheSize: Int
    get() = cache.size

  override fun offset(timestamp: @ms Double, timeZone: TimeZone): @ms Double {
    val alignedTimestamp = alignTimestampToTimeZones(timestamp)
    //Calculate the hash code to avoid instantiation of objects.
    val key = 31 * alignedTimestamp.hashCode() + timeZone.hashCode()
    return cache.getOrStore(key) {
      delegate.offset(timestamp, timeZone)
    }
  }

  companion object {
    private val fifteenMinutes: @ms Double = 15.minutes.toDouble(DurationUnit.MILLISECONDS)

    internal fun alignTimestampToTimeZones(timestamp: @ms Double): @ms Double {
      //While most time zones differ from Coordinated Universal Time (UTC) by a number
      //of full hours, there are also a few time zones with both 30-minute and 45-minute
      //offsets. So a precision of 15 minutes should suffice.
      //We use floor() here because a time change (switch from summer to winter savings time)
      //typically occurs to the full hour.
      return (timestamp / fifteenMinutes).floor() * fifteenMinutes
    }
  }
}
