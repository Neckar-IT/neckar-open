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
package it.neckar.ktor.client.heartbeat

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.neckar.heartbeat.HeartbeatCheck
import it.neckar.heartbeat.HeartbeatState
import it.neckar.ktor.client.featureFlagsHeader
import it.neckar.ktor.client.get
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.http.Url
import it.neckar.problem.ApplicationError
import it.neckar.problem.ExpectedApplicationError
import it.neckar.problem.UnexpectedApplicationError
import it.neckar.rest.version.RestProtocolVersionProblemException.Cause.InvalidClientVersion
import it.neckar.rest.version.RestProtocolVersionProblemException.Cause.MissingClientVersion
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Verifies the response. Returns the heartbeat state depending on the result.
 */
typealias ResponseVerification = suspend (HttpResponse, responseTime: Duration) -> HeartbeatState

/**
 * Uses the provided HTTP client to check the availability of the service
 */
class HttpClientHeartbeatCheck(
  /**
   * The URL that is checked
   */
  val heartbeatUrl: Url.Absolute,

  /**
   * The client that is used to check the heartbeat.
   * Should be configured with the necessary settings - e.g., authorization if necessary
   */
  val httpClient: HttpClient,

  /**
   * Callback to verify the HTTP response.
   * The callback is *only* called for successful responses.
   *
   * A response with a status code >= 400 counts as dead without calling the [responseVerification].
   * The client is created with `expectSuccess = true`, so such a response arrives as a
   * [ResponseException]: [ClientRequestException] for a 4xx, [ServerResponseException] for a 5xx.
   */
  val responseVerification: ResponseVerification = { _, responseTime ->
    HeartbeatState.Alive(responseTime)
  },

  ) : HeartbeatCheck {
  override val description: String
    get() = heartbeatUrl.value

  override suspend fun check(): HeartbeatState {
    try {
      val result: HttpResponse
      val duration = measureTime {
        result = httpClient.get(heartbeatUrl) {
          featureFlagsHeader()
        }
      }
      logger.debug("result: $result")

      return responseVerification(result, duration)
    } catch (e: ResponseException) {
      val bodyAsText = e.response.bodyAsText()
      val statusCode: HttpStatusCode = e.response.status
      try {
        when (val applicationError = Json.decodeFromString(ApplicationError.serializer(), bodyAsText)) {
          is ExpectedApplicationError -> {
            return when (applicationError.errorCode) {
              MissingClientVersion.errorCode -> HeartbeatState.Dead.VersionMismatch
              InvalidClientVersion.errorCode -> HeartbeatState.Dead.VersionMismatch
              else -> HeartbeatState.Dead.ErrorResponse(
                statusCode = statusCode.value,
                message = applicationError.fallbackMessage
              )
            }
          }

          is UnexpectedApplicationError -> return HeartbeatState.Dead.ErrorResponse(
            statusCode = statusCode.value,
            message = applicationError.fallbackMessage
          )
        }
      } catch (innerException: Exception) {
        logger.warn("Could not handle problem response: $bodyAsText", innerException)
      }

      return HeartbeatState.Dead.ErrorResponse(
        statusCode = statusCode.value,
        message = bodyAsText
      )
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.client.heartbeat.HttpClientHeartbeatCheck")
  }
}
