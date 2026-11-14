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

import assertk.*
import assertk.assertions.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import it.neckar.heartbeat.HeartbeatState
import it.neckar.ktor.client.createHttpClient
import it.neckar.open.exceptions.PermissionDeniedException
import it.neckar.open.http.Url
import it.neckar.open.version.Version
import it.neckar.problem.ApplicationError
import it.neckar.problem.toExpectedProblem
import it.neckar.problem.toUnexpectedProblem
import it.neckar.rest.version.RestProtocolVersionProblemException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 *
 */
class HttpClientHeartbeatCheckTest {
  @Test
  suspend fun testHeartbeat400() {
    val heartbeatUrl = Url.absolute("http://localhost:8080/heartbeat")

    val httpClient = createHttpClient(MockEngine { _: HttpRequestData ->
      respond(
        content = Json.encodeToString(ApplicationError.serializer(), IllegalStateException("Uups").toUnexpectedProblem()),
        status = HttpStatusCode.BadRequest
      )

    }, "test-client") { }


    val responseVerification: ResponseVerification = { response, responseTime ->
      HeartbeatState.Dead.ErrorResponse(response.status.value, "an error")
    }

    val heartbeatCheck = HttpClientHeartbeatCheck(heartbeatUrl, httpClient, responseVerification)
    assertThat(heartbeatCheck.check()).isErrorResponse(statusCode = 400, message = "Uups")
  }

  @Test
  suspend fun `a service unavailable answer is dead, not an exception`() {
    val heartbeatUrl = Url.absolute("http://localhost:8080/heartbeat")

    val httpClient = createHttpClient(MockEngine { _: HttpRequestData ->
      respond(
        content = Json.encodeToString(ApplicationError.serializer(), IllegalStateException("not ready").toUnexpectedProblem()),
        status = HttpStatusCode.ServiceUnavailable,
      )
    }, "test-client") { }

    val heartbeatCheck = HttpClientHeartbeatCheck(heartbeatUrl, httpClient)

    assertThat(heartbeatCheck.check()).isErrorResponse(statusCode = 503, message = "not ready")
  }

  @Test
  suspend fun testHeartbeatForbidden() {
    val heartbeatUrl = Url.absolute("http://localhost:8080/heartbeat")

    val httpClient = createHttpClient(MockEngine { _: HttpRequestData ->
      respond(
        content = Json.encodeToString(ApplicationError.serializer(), PermissionDeniedException("not allowed!").toUnexpectedProblem()),
        status = HttpStatusCode.Forbidden,
      )
    }, "test-client") { }


    val responseVerification: ResponseVerification = { response, responseTime ->
      HeartbeatState.Dead.ErrorResponse(response.status.value, "an error")
    }

    val heartbeatCheck = HttpClientHeartbeatCheck(heartbeatUrl, httpClient, responseVerification)
    assertThat(heartbeatCheck.check()).isErrorResponse(statusCode = 403, message = "not allowed!")
  }

  @Test
  suspend fun testHeartbeatClientVersionMissing() {
    val heartbeatUrl = Url.absolute("http://localhost:8080/heartbeat")

    val httpClient = createHttpClient(MockEngine { _: HttpRequestData ->
      respond(
        content = Json.encodeToString(
          ApplicationError.serializer(),
          RestProtocolVersionProblemException.invalidClientVersion(Version(1, 2, 3), Version(7, 9, 9)).toExpectedProblem()
        ),
        status = HttpStatusCode.Forbidden
      )
    }, "test-client") { }


    val responseVerification: ResponseVerification = { _, _ ->
      fail("should not be called")
    }

    val heartbeatCheck = HttpClientHeartbeatCheck(heartbeatUrl, httpClient, responseVerification)
    assertThat(heartbeatCheck.check()).isVersionMismatch()
  }

  @Test
  suspend fun testOkDefault() {
    val heartbeatUrl = Url.absolute("http://localhost:8080/heartbeat")

    val httpClient = createHttpClient(MockEngine { _: HttpRequestData ->
      respond(
        content = "Ok",
        status = HttpStatusCode.OK
      )
    }, "test-client") { }


    val heartbeatCheck = HttpClientHeartbeatCheck(heartbeatUrl, httpClient)

    heartbeatCheck.check().let {
      assertThat(it).isAlive()
      it as HeartbeatState.Alive
      assertThat(it.responseTime).isGreaterThan(0.milliseconds)
      assertThat(it.responseTime).isLessThan(1.minutes)
    }
  }
}
