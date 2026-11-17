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

import it.neckar.commons.kotlin.js.LocalStorageKey
import it.neckar.commons.kotlin.js.LocalStorageSupport
import it.neckar.commons.kotlin.js.exception.unescape
import it.neckar.commons.kotlin.js.getUrlParameter
import kotlinx.browser.window
import kotlinx.serialization.builtins.serializer


/**
 * Writes the currently set Feature Flags into the URL. Does not refresh the page.
 */
fun FeatureFlagsSupport.writeToUrl() {
  flags.writeToUrl()
}

/**
 * Writes the feature flags into the URL. Does not refresh the page.
 */
fun FeatureFlags.writeToUrl() {
  val flagsAsString = encodeToString()
  val urlWithoutFeatureFlags = getUrlWithoutFeatureFlags(window.location.href)
  if (flagsAsString.isEmpty()) {
    window.history.replaceState(null, "", urlWithoutFeatureFlags)
  } else {
    window.history.replaceState(null, "", "$urlWithoutFeatureFlags?${FeatureFlags.QueryParamName}=$flagsAsString")
  }
}

/**
 * Parses the feature flags from the URL and sets the current flags
 */
fun FeatureFlagsSupport.parseFromUrl() {
  /**
   * Parse feature flags URL
   */
  this.flags = parseFeatureFlagsFromUrl() ?: FeatureFlags.empty
}

/**
 * Parses the feature flags from the URL.
 * Returns null if no url parameter could be found
 */
fun parseFeatureFlagsFromUrl(): FeatureFlags? {
  val urlParameter = getUrlParameter(FeatureFlags.QueryParamName) ?: return null
  return FeatureFlags.decodeFromString(urlParameter)
}

/**
 * Returns a String without the feature Flags.
 */
fun getUrlWithoutFeatureFlags(url: String): String {
  val result = url.substringBefore("&${FeatureFlags.QueryParamName}=")
  return result.substringBefore("?${FeatureFlags.QueryParamName}=")
}


/**
 * Writes the feature flags to local storage
 */
fun FeatureFlags.writeToLocalStorage() {
  LocalStorageSupport.saveToLocalStorage(featureFlagsLocalStorageKey, this.encodeToString(), String.serializer())
}

/**
 * Loads the feature flags from local storage.
 * Returns null if no feature flags have been stored
 */
fun loadFeatureFlagsFromLocalStorage(): FeatureFlags? {
  val featureFlagsAsString = LocalStorageSupport.loadFromLocalStorage(featureFlagsLocalStorageKey, String.serializer()) ?: return null
  return FeatureFlags.decodeFromString(featureFlagsAsString)
}

val featureFlagsLocalStorageKey: LocalStorageKey = LocalStorageKey("featureFlags")


/**
 * Loads the feature flags from URL or local storage.
 *
 * Checks for feature flags in this order (returns the first hit):
 * - URL
 * - Local Storage
 *
 * Returns null if no feature flags could be found in any of the places
 */
fun loadFeatureFlagsFromUrlOrLocalStorage(): FeatureFlags? {
  val featureFlagsFromUrl = parseFeatureFlagsFromUrl()

  if (featureFlagsFromUrl != null) {
    return featureFlagsFromUrl
  }

  val featureFlagsFromLocalStorage = loadFeatureFlagsFromLocalStorage()
  if (featureFlagsFromLocalStorage != null) {
    return featureFlagsFromLocalStorage
  }

  return null
}
