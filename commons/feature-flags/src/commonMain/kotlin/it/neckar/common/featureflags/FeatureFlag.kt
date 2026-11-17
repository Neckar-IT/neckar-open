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

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Describes a feature flag.
 *
 * Feature flags are categorized by purpose: [ReleaseToggle], [OpsToggle],
 * [ExperimentToggle], [PermissionToggle].
 *
 * Currently client-controlled (DevTools, URL params, localStorage).
 * Server-side management (per user/tenant) is a planned extension.
 */
@Serializable
data class FeatureFlag(
  /**
   * The key of the feature flag.
   * Feature flags are *only* identified by their key.
   */
  val key: Key,
  /**
   * The *optional* description.
   *
   * Not part of the flag's identity: [equals] and [hashCode] compare the [key] only, and
   * [FeatureFlags.encodeToString] omits it — a flag decoded from a request header, a URL parameter
   * or local storage therefore carries `null` here.
   *
   * The `feature-flags/available` endpoint is the one place a description reaches a client:
   * it serializes [FeatureFlag.available] with the descriptions, so a client can label the flags it offers.
   */
  val description: String? = null,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FeatureFlag) return false

    if (key != other.key) return false

    return true
  }

  override fun hashCode(): Int {
    return key.hashCode()
  }

  /**
   * Returns true if this feature flag is active
   */
  fun active(): Boolean {
    return FeatureFlagsSupport.active(this)
  }

  operator fun invoke(): Boolean {
    return active()
  }

  companion object {
    /**
     * Simulate a delay in UI actions.
     * This feature can be used to test busy indicators.
     *
     * Every async action will be delayed for 1 sec
     */
    @OpsToggle
    val slowUi: FeatureFlag = FeatureFlag("slowUI", "Delay actions in the UI")

    /**
     * Simulates a delay on HTTP requests responses on the server side
     */
    @OpsToggle
    val slowServer: FeatureFlag = FeatureFlag("slowServer", "Delay responses on the server")

    /**
     * Delays every HTTP response by a DSL line's latency with margin (20–60 ms), through `FeatureFlagsPlugin` or, without it, `handle()`.
     * Default of every local service `run` (Gradle property `localDevFeatureFlags`).
     */
    @OpsToggle
    val realisticLatency: FeatureFlag = FeatureFlag("realisticLatency", "Delay responses on the server by the latency of a DSL line")

    @OpsToggle
    val forceServerError: FeatureFlag = FeatureFlag("forceServerError", "On Client Request force an internal server error")

    /**
     * Can be used to simulate failure of refreshing the access token
     */
    @OpsToggle
    val forceFailRefreshAccessToken: FeatureFlag = FeatureFlag("forceFailRefreshAccessToken", "Forces failure of refreshing the access token")


    @OpsToggle
    val tokenDurationVeryShort: FeatureFlag = FeatureFlag("tokenDurationVeryShort", "Reduces the duration for the access/refresh tokens - for testing purposes")

    /**
     * Enable / disable testing
     */
    @OpsToggle
    val testing: FeatureFlag = FeatureFlag("testing", "Testing - non production")

    @OpsToggle
    val forceFailEssentialsLoading: FeatureFlag = FeatureFlag("forceFailEssentialsLoading", "Forces the failure of loading the essentials")

    /**
     * Disables the version check on the server and client
     */
    @OpsToggle
    val disableVersionCheck: FeatureFlag = FeatureFlag("disableVersionCheck", "Disable the version check for client and/or server")

    /**
     * Forces the version check to fail
     */
    @OpsToggle
    val forceFailVersionCheck: FeatureFlag = FeatureFlag("forceFailVersionCheck", "Force to fail the version check")

    @OpsToggle
    val throwException: FeatureFlag = FeatureFlag("throwException", "Throws an exception - to test error handling")

    /**
     * Simulates a slow Keycloak initialization by adding a 5-second delay before kc.init().
     * Useful to inspect the pending state before the Keycloak redirect.
     */
    @OpsToggle
    val simulateKeycloakSlow: FeatureFlag = FeatureFlag("simulateKeycloakSlow", "Add a 5s delay before Keycloak init")

    /**
     * Simulates a Keycloak outage by replacing the Keycloak URL with an invalid one.
     * Triggers the Keycloak init failed error state.
     */
    @OpsToggle
    val simulateKeycloakDown: FeatureFlag = FeatureFlag("simulateKeycloakDown", "Replace Keycloak URL with an invalid one")

    /**
     * Contains the available feature flags
     */
    val available: List<FeatureFlag> = listOf(
      slowUi,
      slowServer,
      realisticLatency,
      forceServerError,
      forceFailRefreshAccessToken,
      tokenDurationVeryShort,
      testing,
      forceFailEssentialsLoading,
      disableVersionCheck,
      forceFailVersionCheck,
      throwException,
      simulateKeycloakSlow,
      simulateKeycloakDown,
    )

    operator fun invoke(key: String, description: String? = null): FeatureFlag {
      return FeatureFlag(Key(key), description)
    }

    inline fun forKey(key: String): FeatureFlag {
      return forKey(Key(key))
    }

    /**
     * Returns the feature flag for the given key
     */
    fun forKey(key: Key): FeatureFlag {
      return available.find {
        it.key == key
      } ?: FeatureFlag(key)
    }
  }

  /**
   * The key that identifies a feature flag.
   * Keys must not contain commas as they are used in comma-separated encoding.
   */
  @Serializable
  @JvmInline
  value class Key(val value: String) {
    init {
      require(value.contains(",").not()) {
        """Key must not contain ",""""
      }
    }

    override fun toString(): String = value
  }
}
