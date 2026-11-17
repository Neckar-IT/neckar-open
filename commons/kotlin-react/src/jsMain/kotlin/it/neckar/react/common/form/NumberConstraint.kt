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
package it.neckar.react.common.form

import it.neckar.open.unit.other.Inclusive

/**
 * A sealed interface defining number constraints that can be applied to both integers and doubles.
 */
sealed interface NumberConstraint {
  /**
   * Applies a constraint to an integer value.
   *
   * @param value the integer value to apply the constraint to.
   * @return the constrained integer value.
   */
  fun constraint(value: Int): Int

  /**
   * Applies a constraint to a double value.
   *
   * @param value the double value to apply the constraint to.
   * @return the constrained double value.
   */
  fun constraint(value: Double): Double
}

/**
 * Does *not* apply any constraint
 */
data object Unconstrained : NumberConstraint {
  override fun constraint(value: Int): Int {
    return value
  }

  override fun constraint(value: Double): Double {
    return value
  }
}

/**
 * Returns at least zero
 */
data object ZeroOrPositive : NumberConstraint {
  override fun constraint(value: Int): Int {
    return value.coerceAtLeast(0)
  }

  override fun constraint(value: Double): Double {
    return value.coerceAtLeast(0.0)
  }
}

/**
 * Keeps a number within the provided values
 */
class CoerceIn(
  val lowerLimit: @Inclusive Int,
  val upperLimit: @Inclusive Int,
) : NumberConstraint {
  init {
    require(lowerLimit <= upperLimit) {
      "Lower constraint $lowerLimit needs to be lower (or the same) as that upper constraint $upperLimit but was not!"
    }
  }

  override fun constraint(value: Int): Int {
    return value.coerceIn(lowerLimit, upperLimit)
  }

  override fun constraint(value: Double): Double {
    return value.coerceIn(lowerLimit.toDouble(), upperLimit.toDouble())
  }
}
