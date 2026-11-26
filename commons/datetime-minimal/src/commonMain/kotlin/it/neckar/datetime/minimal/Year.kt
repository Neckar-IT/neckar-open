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

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A calendar year, counted astronomically: year 0 is 1 BC, `Year(-43)` is 44 BC.
 *
 * The range [Min]..[Max] covers the age of the universe with margin; what counts as a plausible
 * year for a single field (a year of birth, of commissioning) is a property of that field.
 */
@JvmInline
@Serializable
value class Year(val value: Long) : Comparable<Year> {
  constructor(value: Int) : this(value.toLong())

  init {
    require(value in MinValue..MaxValue) {
      "Year $value outside $MinValue..$MaxValue"
    }
  }

  /**
   * Proleptic gregorian leap year rule, applied to every year.
   */
  fun isLeapYear(): Boolean {
    return (value % 4L == 0L && value % 100L != 0L) || (value % 400L == 0L)
  }

  /**
   * The value as [Int] for callers that only handle years of the common era range.
   */
  fun toInt(): Int {
    require(value in Int.MIN_VALUE..Int.MAX_VALUE) {
      "Year $value outside ${Int.MIN_VALUE}..${Int.MAX_VALUE}"
    }
    return value.toInt()
  }

  override fun compareTo(other: Year): Int {
    return value.compareTo(other.value)
  }

  operator fun plus(n: Int): Year {
    return Year(value + n)
  }

  operator fun plus(n: Long): Year {
    return Year(value + n)
  }

  operator fun minus(other: Year): Year {
    return Year(value - other.value)
  }

  operator fun minus(other: Int): Year {
    return Year(value - other)
  }

  override fun toString(): String {
    return value.toString()
  }

  companion object {
    /**
     * The smallest supported year, before the Big Bang 13.787 billion years ago.
     */
    const val MinValue: Long = -15_000_000_000L

    /**
     * The largest supported year.
     */
    const val MaxValue: Long = 15_000_000_000L

    val Min: Year = Year(MinValue)
    val Max: Year = Year(MaxValue)
  }
}
