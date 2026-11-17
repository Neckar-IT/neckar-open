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
package it.neckar.rest.version

import it.neckar.open.exceptions.ApplicationException
import it.neckar.open.exceptions.ErrorCode
import it.neckar.open.version.Version
import it.neckar.open.version.VersionRange

/**
 * Is thrown if there is a problem with the protocol version
 */
class RestProtocolVersionProblemException(
  val details: Cause,
  cause: Throwable? = null,
  override val fallbackMessage: String,
  override val parameters: Map<String, Any>? = null,
) : ApplicationException(cause) {

  override val errorCode: ErrorCode
    get() = details.errorCode

  /**
   * The cause for the client version problem
   */
  enum class Cause(
    val errorCode: ErrorCode,
  ) {
    MissingClientVersion(ErrorCode.create("PVE", 100)),
    InvalidClientVersion(ErrorCode.create("PVE", 101)),
    ;
  }

  companion object {
    fun missingClientVersion(
      clientHeaderKey: String,
      cause: Throwable? = null,
    ): RestProtocolVersionProblemException {
      return RestProtocolVersionProblemException(
        details = Cause.MissingClientVersion,
        cause = cause,
        fallbackMessage = "Client not supported. Missing client version header (${clientHeaderKey})",
        parameters = mapOf(
          "clientHeaderKey" to clientHeaderKey
        ),
      )
    }

    fun invalidClientVersion(
      expectedVersionRange: VersionRange,
      clientVersion: Version,
      cause: Throwable? = null,
    ): RestProtocolVersionProblemException {
      return RestProtocolVersionProblemException(
        Cause.InvalidClientVersion,
        cause = cause,
        fallbackMessage = "Client not supported. Expected client version ${expectedVersionRange.format()} but got $clientVersion",
        parameters = mapOf(
          "expectedVersion" to expectedVersionRange.format(),
          "clientVersion" to clientVersion,
        )
      )
    }

    fun invalidClientVersion(
      expectedVersion: Version,
      clientVersion: Version,
      cause: Throwable? = null,
    ): RestProtocolVersionProblemException {
      return invalidClientVersion(expectedVersion.format(), clientVersion.format(), cause)
    }

    fun invalidClientVersion(
      expectedVersionString: String,
      clientVersionString: String,
      cause: Throwable? = null,
    ): RestProtocolVersionProblemException {
      return RestProtocolVersionProblemException(
        Cause.InvalidClientVersion,
        cause = cause,
        fallbackMessage = "Client not supported. Expected client version $expectedVersionString but got $clientVersionString",
        parameters = mapOf(
          "expectedVersion" to expectedVersionString,
          "clientVersion" to clientVersionString,
        )
      )
    }
  }
}
