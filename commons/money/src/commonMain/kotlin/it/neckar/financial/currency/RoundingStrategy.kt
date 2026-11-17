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
package it.neckar.financial.currency

import kotlin.math.abs
import kotlin.math.sign

/**
 * How a fraction of the smallest unit becomes a whole unit. Which strategy applies is a property of the jurisdiction a
 * calculation falls under; [Default] is the German commercial rounding.
 */
enum class RoundingStrategy {
  /**
   * Commercial rounding ("kaufmännisch"): a half moves away from zero, -199.5 to -200.
   */
  HalfAwayFromZero {
    override fun tieRoundsAwayFromZero(truncated: Long): Boolean {
      return true
    }
  },

  /**
   * A half goes to the even neighbour, 151.5 to 152 and 154.5 to 154.
   */
  HalfToEven {
    override fun tieRoundsAwayFromZero(truncated: Long): Boolean {
      return truncated % 2 != 0L
    }
  };

  fun roundToLong(value: Double): Long {
    // toLong() would turn NaN into 0 and an infinity or a value beyond Long into Long.MIN_VALUE or Long.MAX_VALUE.
    require(value.isFinite() && abs(value) < Long.MAX_VALUE.toDouble()) { "Cannot round $value" }
    val truncated: Long = value.toLong()
    val fraction: Double = abs(value - truncated)
    return roundTruncated(truncated, fraction.compareTo(0.5), sign(value).toLong())
  }

  /**
   * Rounds [numerator] / [denominator] in integer arithmetic, so a tie is decided exactly.
   */
  fun roundQuotient(numerator: Long, denominator: Long): Long {
    require(denominator > 0L) { "Denominator must be positive: $denominator" }
    val truncated: Long = numerator / denominator
    val remainder: Long = abs(numerator % denominator)
    return roundTruncated(truncated, remainder.compareTo(denominator - remainder), numerator.sign.toLong())
  }

  /**
   * [fractionComparedToHalf] is the sign of (fraction - 0.5); only a tie asks the strategy.
   */
  private fun roundTruncated(truncated: Long, fractionComparedToHalf: Int, sign: Long): Long {
    val roundsAwayFromZero: Boolean = when {
      fractionComparedToHalf > 0 -> true
      fractionComparedToHalf < 0 -> false
      else -> tieRoundsAwayFromZero(truncated)
    }
    return if (roundsAwayFromZero) truncated + sign else truncated
  }

  /**
   * Whether a value exactly halfway between [truncated] and the next whole number away from zero moves away.
   */
  protected abstract fun tieRoundsAwayFromZero(truncated: Long): Boolean

  companion object {
    val Default: RoundingStrategy = HalfAwayFromZero
  }
}
