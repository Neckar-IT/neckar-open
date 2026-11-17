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
package it.neckar.common.featureflags

/**
 * Contains a set of feature flags
 */
data class FeatureFlags(
  /**
   * Set of feature flags
   */
  val flags: Set<FeatureFlag>,
) {
  /**
   * Returns true if this contains the given feature flag
   */
  fun contains(featureFlag: FeatureFlag): Boolean {
    return this.flags.contains(featureFlag)
  }

  constructor(vararg flags: FeatureFlag) : this(flags.toSet())

  override fun toString(): String {
    return "[${flags.joinToString(", ") { it.key.value }}]"
  }

  /**
   * Encodes the feature flags as string
   */
  fun encodeToString(): String {
    return flags.joinToString(",") { it.key.value }
  }

  /**
   * Creates a new [FeatureFlags] instance with the additional feature flag
   */
  fun withAdditional(additionalFlag: FeatureFlag): FeatureFlags {
    return FeatureFlags(this.flags + additionalFlag)
  }

  operator fun plus(additionalFlag: FeatureFlag): FeatureFlags {
    return withAdditional(additionalFlag)
  }

  /**
   * Creates a new [FeatureFlags] instance without the given feature flag
   */
  fun withRemovedFlag(flagToBeRemoved: FeatureFlag): FeatureFlags {
    return FeatureFlags(this.flags - flagToBeRemoved)
  }

  fun isEmpty(): Boolean {
    return flags.isEmpty()
  }

  /**
   * Creates a new [FeatureFlags] instance with the additional feature flags
   */
  operator fun plus(otherFlags: FeatureFlags): FeatureFlags {
    if (otherFlags.isEmpty()) {
      return this
    }

    if (this.isEmpty()) {
      return otherFlags
    }

    return FeatureFlags(flags + otherFlags.flags)
  }

  companion object {
    /**
     * The HTTP header key used for feature flags.
     *
     * Mirrors `it.neckar.rest.PlatformHeader.FeatureFlags.headerName` — kept as a separate
     * `const val` because `feature-flags` does not depend on `rest-model`. The match is
     * enforced by `PlatformHeaderTest.feature flags header key matches platform header`.
     */
    const val HeaderKey: String = "NeckarIT-Feature-Flags"

    /**
     * The URL query parameter name used for feature flags
     */
    const val QueryParamName: String = "featureFlags"

    /**
     * Contains no feature flags
     */
    val empty: FeatureFlags = FeatureFlags(emptySet())


    /**
     * Parses the feature flags from a string created by [encodeToString]
     */
    fun decodeFromString(asString: String?): FeatureFlags {
      if (asString == null) {
        return empty
      }

      return FeatureFlags(
        asString.split(",")
          .filter { it.isNotEmpty() }   // empty input or trailing comma would otherwise produce a blank flag key
          .map {
            FeatureFlag.forKey(it)
          }.toSet()
      )
    }
  }
}
