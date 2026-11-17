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
package it.neckar.adjustable

import it.neckar.open.annotations.CrossLayer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a value that is either calculated, calculated and overridden by the user, or set by the user alone.
 *
 * The variant answers two questions at once: which value is effective ([value]) and whether a calculated
 * alternative exists to fall back to. Only [Adjusted] carries both, so only [Adjusted] can be reset.
 */
@CrossLayer("A stored, overridable value that entities and views carry unchanged")
@Serializable
sealed interface AdjustableValue<out T> {
  /**
   * The current value - either the calculated or the overridden one. Depending on the type
   */
  val value: T

  /**
   * The calculated value behind this one, or null if there is none.
   *
   * Answers "is there something to fall back to?" without knowing the variants: [Auto] and [Adjusted] have a
   * calculated value, [Manual] does not.
   */
  fun findCalculated(): T?

  /**
   * The automatically calculated value has been selected (and not been adjusted by the user)
   */
  @Serializable
  @SerialName("Auto")
  data class Auto<out T>(
    /**
     * The calculated value
     */
    override val value: T
  ) : AdjustableValue<T> {

    /**
     * Helper to get the calculated value
     */
    val calculated: T
      get() = value

    override fun findCalculated(): T = calculated
  }

  /**
   * The user has adjusted the value
   */
  @Serializable
  @SerialName("Adjusted")
  data class Adjusted<out T>(
    /**
     * The calculated value
     */
    val calculated: T,
    /**
     * The overridden value from the user
     */
    override val value: T,
  ) : AdjustableValue<T> {
    /**
     * Helper to get the adjusted value
     */
    val adjustedValue: T
      get() = value

    override fun findCalculated(): T = calculated
  }

  /**
   * The user has provided the value and no calculated value exists for it.
   *
   * This is not an adjustment: there is nothing to compare against and nothing to reset to. Use this instead of
   * [Auto] whenever a value is entered by hand while the calculation has no result - claiming it was calculated
   * would make the origin of the value a lie and would offer the user a reset that silently keeps their own input.
   */
  @Serializable
  @SerialName("Manual")
  data class Manual<out T>(
    /**
     * The value the user provided
     */
    override val value: T
  ) : AdjustableValue<T> {
    override fun findCalculated(): T? = null
  }

  companion object {
    /**
     * Creates a calculated value
     */
    fun <T> auto(calculated: T): AdjustableValue<T> {
      return Auto(calculated)
    }

    /**
     * Creates an adjusted value
     */
    fun <T> adjusted(calculated: T, adjustedValue: T): AdjustableValue<T> {
      return Adjusted(calculated, adjustedValue)
    }

    /**
     * Creates a value provided by the user without a calculated counterpart
     */
    fun <T> manual(value: T): AdjustableValue<T> {
      return Manual(value)
    }
  }
}

/**
 * Returns this value with the calculated side replaced by [newCalculated], keeping the user's choice intact.
 *
 * This is what a recalculation does: the suggestion is refreshed, an override survives it. A [Manual] value is
 * promoted to [Adjusted] - a calculation now exists, so the user's value becomes an adjustment of it and can be reset.
 */
fun <T> AdjustableValue<T>.withCalculated(newCalculated: T): AdjustableValue<T> {
  return when (this) {
    is AdjustableValue.Auto -> AdjustableValue.auto(newCalculated)
    is AdjustableValue.Adjusted -> AdjustableValue.adjusted(newCalculated, value)
    is AdjustableValue.Manual -> AdjustableValue.adjusted(newCalculated, value)
  }
}

/**
 * Refreshes the calculated side where the calculation may have no result and nothing may be stored yet -
 * the two cases [withCalculated] cannot express.
 *
 * With a result this is [withCalculated], starting at [AdjustableValue.Auto] when nothing is stored.
 * Without a result only a value the user typed survives - as [AdjustableValue.Manual], because there is
 * no longer a calculation it could be an adjustment of.
 */
fun <T : Any> AdjustableValue<T>?.recalculated(newCalculated: T?): AdjustableValue<T>? {
  return when {
    newCalculated != null -> this?.withCalculated(newCalculated) ?: AdjustableValue.auto(newCalculated)
    this is AdjustableValue.Adjusted -> AdjustableValue.manual(value)
    this is AdjustableValue.Manual -> this
    // Auto carried nothing but the calculation that just disappeared, and null stays null.
    else -> null
  }
}

/**
 * Applies [transform] to every value this wrapper carries, keeping the variant.
 *
 * Narrows the type inside the wrapper without an unchecked cast of the wrapper itself.
 */
fun <T, R> AdjustableValue<T>.map(transform: (T) -> R): AdjustableValue<R> {
  return when (this) {
    is AdjustableValue.Auto -> AdjustableValue.auto(transform(value))
    is AdjustableValue.Adjusted -> AdjustableValue.adjusted(transform(calculated), transform(value))
    is AdjustableValue.Manual -> AdjustableValue.manual(transform(value))
  }
}

/**
 * Discards the user's override and returns to the calculated value.
 *
 * [AdjustableValue.Auto] and [AdjustableValue.Manual] are returned unchanged - neither carries a second value to
 * fall back to, so this does not always end up on [AdjustableValue.Auto].
 */
fun <T> AdjustableValue<T>.resetToCalculated(): AdjustableValue<T> {
  return when (this) {
    is AdjustableValue.Auto -> this
    is AdjustableValue.Adjusted -> AdjustableValue.auto(calculated)
    is AdjustableValue.Manual -> this
  }
}

/**
 * Wraps this calculated value as an [AdjustableValue.Auto]
 */
fun <T> T.asAuto(): AdjustableValue<T> {
  return AdjustableValue.auto(this)
}

/**
 * Wraps this calculated value as an [AdjustableValue.Adjusted] that the user overrode with [adjustedValue]
 */
fun <T> T.asAdjusted(adjustedValue: T): AdjustableValue<T> {
  return AdjustableValue.adjusted(this, adjustedValue)
}

/**
 * Wraps this user-provided value as an [AdjustableValue.Manual] - use this whenever no calculation stands behind it
 */
fun <T> T.asManual(): AdjustableValue<T> {
  return AdjustableValue.manual(this)
}
