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
package it.neckar.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import it.neckar.common.featureflags.FeatureFlag
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.common.featureflags.FeatureFlagsSupport
import it.neckar.open.version.Version
import it.neckar.open.version.VersionInformation
import it.neckar.open.version.VersionRange
import it.neckar.rest.ClientRole
import it.neckar.rest.tracking.hasAnyClientRole
import it.neckar.rest.version.ProtocolFingerprint
import it.neckar.rest.version.RestProtocolVersionProblemException
import it.neckar.rest.version.RestProtocolVersionInfo

/**
 * Checks the client version number provided as header.
 *
 * A request without the header passes: only a stated version can disagree with ours, and callers
 * outside this build — monitoring probes, a browser opening a URL — have none to state.
 */
val CheckClientProtocolVersion: ApplicationPlugin<ClientProtocolVersionCheckerConfiguration> = createApplicationPlugin(
  "Check client protocol version",
  createConfiguration = { ClientProtocolVersionCheckerConfiguration() }
) {

  onCall { call ->
    val featureFlags = call.extractFeatureFlags()

    if (featureFlags.contains(FeatureFlag.disableVersionCheck)) {
      return@onCall
    }

    if (call.request.isCorsRequest) {
      //Skip for options to allow verification of CORS
      return@onCall
    }

    //Check if the request should be included in the check
    if (pluginConfig.includeFilter(call.request).not()) {
      return@onCall
    }

    if (call.request.headers.contains(pluginConfig.protocolVersionHeaderKey).not()) {
      call.response.header("${pluginConfig.protocolVersionHeaderKey}-Check-Skipped", "Version check skipped: the client states no version")
      return@onCall
    }

    val clientVersionString = getProtocolVersionString(call.request.headers, pluginConfig.protocolVersionHeaderKey, featureFlags)

    /**
     * The check needs real values on both sides: [VersionInformation.UnknownValue] means no
     * deploy metadata is injected (local development, HTML served without the injecting
     * entrypoint) - skip instead of failing spuriously.
     */
    // A configured version string speaks for itself; only without one does the git hash stand in.
    val expectedVersionUnresolved: Boolean = pluginConfig.expectedVersionString
      ?.let { it == VersionInformation.UnknownValue }
      ?: (VersionInformation.gitHash == VersionInformation.UnknownValue)

    if (clientVersionString == VersionInformation.UnknownValue || expectedVersionUnresolved) {
      call.response.header("${pluginConfig.protocolVersionHeaderKey}-Check-Skipped", "Version check skipped: client or server has no injected deploy metadata (unknown)")
      return@onCall
    }

    //lambda will throw an exception
    pluginConfig.versionValidator(clientVersionString)
  }
}

/**
 * Returns the client version number from the request.
 * Throws an exception if the version number is missing.
 */
fun getProtocolVersionString(
  headers: Headers,
  protocolVersionHeaderKey: String = RestProtocolVersionInfo.ProtocolVersionHeaderKey,
  featureFlags: FeatureFlags = FeatureFlagsSupport.flags,
): String {
  val clientVersionString = headers[protocolVersionHeaderKey] ?: throw RestProtocolVersionProblemException.missingClientVersion(protocolVersionHeaderKey)

  if (featureFlags.contains(FeatureFlag.forceFailVersionCheck)) {
    //Force the exception
    throw RestProtocolVersionProblemException.invalidClientVersion("forced-failure", clientVersionString)
  }

  return clientVersionString
}

/**
 * Configuration for a client version checker.
 */
class ClientProtocolVersionCheckerConfiguration {
  /**
   * Returns true, if one necessary field is set
   */
  val isValid: Boolean
    get() {
      return expectedVersion != null || expectedVersionRange != null || expectedVersionString != null
    }

  /**
   * The expected version range
   */
  var expectedVersionRange: VersionRange? = null
    set(value) {
      field = value

      versionValidator = if (value != null) {
        VersionRangeChecker(value)::invoke
      } else {
        { }
      }
    }

  /**
   * The expected *exact* version
   */
  var expectedVersion: Version? = null
    set(value) {
      field = value

      versionValidator = if (value != null) {
        VersionChecker(value)::invoke
      } else {
        { }
      }
    }

  /**
   * Expected, exact version string
   */
  var expectedVersionString: String? = null
    set(value) {
      field = value

      versionValidator = if (value != null) {
        VersionStringChecker(value)::invoke
      } else {
        {}
      }
    }

  /**
   * The interface this service serves. What a client states is compared against it verbatim.
   */
  var expectedFingerprint: ProtocolFingerprint? = null
    set(value) {
      field = value
      expectedVersionString = value?.value
    }

  /**
   * Header key
   */
  var protocolVersionHeaderKey: String = RestProtocolVersionInfo.ProtocolVersionHeaderKey

  /**
   * Returns true if the request should be included in the check.
   *
   * If this filter returns
   */
  var includeFilter: (ApplicationRequest) -> Boolean = { true }

  /**
   * Sets the provided client roles as include filter
   */
  inline fun forClientRoles(vararg roles: ClientRole) {
    forClientRoles(roles.toSet())
  }

  /**
   * Sets the provided client roles as include filter
   */
  fun forClientRoles(expectedRole: Set<ClientRole>) {
    this.includeFilter = { request ->
      request.headers.hasAnyClientRole(expectedRole)
    }
  }

  /**
   * The lambda that is used to verify the version.
   * The implementation *must* throw an exception if the version does not match
   */
  var versionValidator: (clientVersionString: String) -> Unit = { _ ->
    //noop
  }
}

/**
 * Checks if a client version matches the expected version range
 */
class VersionRangeChecker(val expectedVersionRange: VersionRange) {
  fun invoke(clientVersionString: String) {
    try {
      val clientVersion = Version.parse(clientVersionString)

      if (expectedVersionRange.contains(clientVersion).not()) {
        throw RestProtocolVersionProblemException.invalidClientVersion(expectedVersionRange, clientVersion)
      }
    } catch (e: IllegalArgumentException) {
      throw RestProtocolVersionProblemException.invalidClientVersion(expectedVersionRange.format(), clientVersionString, e)
    }
  }
}

/**
 * Checks for the exact version
 */
class VersionChecker(val expectedVersion: Version) {
  fun invoke(clientVersionString: String) {
    try {
      val clientVersion = Version.parse(clientVersionString)
      if (clientVersion != expectedVersion) {
        throw RestProtocolVersionProblemException.invalidClientVersion(expectedVersion, clientVersion)
      }
    } catch (e: IllegalArgumentException) {
      throw RestProtocolVersionProblemException.invalidClientVersion(expectedVersion.format(), clientVersionString, e)
    }
  }
}

class VersionStringChecker(val expectedVersionString: String) {
  fun invoke(clientVersionString: String) {
    if (expectedVersionString != clientVersionString) {
      throw RestProtocolVersionProblemException.invalidClientVersion(expectedVersionString, clientVersionString)
    }
  }
}

