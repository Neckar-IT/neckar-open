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

import assertk.*
import assertk.assertions.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.ktor.tracking.MDCKeys
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.test.utils.RandomWithSeed
import it.neckar.open.test.utils.SlowTest
import org.junit.jupiter.api.Test

/**
 *
 */
@DisableLogging
@SlowTest
class CallIdTest {
  /**
   * The client does *not* does not set the request id header.
   * The server should generate a new request id.
   */
  @RandomWithSeed
  @Test
  fun testNoClientHeader() = testApplication {
    routing {
      get("/") {
        val requestFromClient = call.request.headers[HttpHeaders.XRequestId]
        assertThat(requestFromClient).isNull()
        call.respondText("asdf")
      }
    }

    lateinit var application: Application
    application {
      application = this
      installCallId()
      installCallLogging()
    }

    startApplication()

    val response = client.get("/")
    assertThat(response).isNotNull()
    assertThat(response.bodyAsText()).isEqualTo("asdf")

    response.headers[HttpHeaders.XRequestId].let {
      assertThat(it).isNotNull()
      assertThat(it.toString()).hasLength(10)
    }
  }

  @RandomWithSeed
  @Test
  fun testClientHeader() = testApplication {
    val clientCallId = "1234567890"

    routing {
      get("/") {
        val requestFromClient = call.request.headers[HttpHeaders.XRequestId]
        assertThat(requestFromClient).isEqualTo(clientCallId)
        assertThat(MDCKeys.CallId.get()).isEqualTo(clientCallId)
        call.respondText("asdf")
      }
    }

    lateinit var application: Application
    application {
      application = this
      installCallId()
      installCallLogging()
    }

    startApplication()

    val response = client.get("/") {
      header(HttpHeaders.XRequestId, clientCallId)
    }

    assertThat(response).isNotNull()
    assertThat(response.bodyAsText()).isEqualTo("asdf")
    assertThat(response.headers[HttpHeaders.XRequestId].toString()).hasLength(10)
    assertThat(response.headers[HttpHeaders.XRequestId].toString()).isEqualTo(clientCallId)
  }

  /**
   * A UUIDv4 request id (contains hyphens) sent by an upstream — e.g. the Traefik edge —
   * must be accepted by the call-id verifier and reused verbatim, not discarded-and-regenerated.
   * Guards the hyphen in the [installCallId] verify alphabet: without it Ktor would generate a
   * different id and break Traefik↔backend log correlation.
   */
  @RandomWithSeed
  @Test
  fun `upstream UUID request id is reused, not regenerated`() = testApplication {
    val upstreamRequestId = "123e4567-e89b-42d3-a456-426614174000"

    routing {
      get("/") {
        assertThat(call.request.headers[HttpHeaders.XRequestId]).isEqualTo(upstreamRequestId)
        assertThat(MDCKeys.CallId.get()).isEqualTo(upstreamRequestId)
        call.respondText("asdf")
      }
    }

    application {
      installCallId()
      installCallLogging()
    }

    startApplication()

    val response = client.get("/") {
      header(HttpHeaders.XRequestId, upstreamRequestId)
    }

    assertThat(response.headers[HttpHeaders.XRequestId]).isEqualTo(upstreamRequestId)
  }

  /**
   * Verifies that the X-Request-Id header appears exactly once in the response,
   * not duplicated.
   */
  @RandomWithSeed
  @Test
  fun `X-Request-Id header must not be duplicated in response`() = testApplication {
    routing {
      get("/") {
        call.respondText("ok")
      }
    }

    application {
      installCallId()
      installCallLogging()
    }

    startApplication()

    val response = client.get("/")
    val xRequestIdValues = response.headers.getAll(HttpHeaders.XRequestId)
    assertThat(xRequestIdValues).isNotNull()
    assertThat(xRequestIdValues!!).hasSize(1)
  }

  /**
   * Verifies that the X-Request-Id header appears exactly once in the response
   * when the client sends a call ID.
   */
  @RandomWithSeed
  @Test
  fun `X-Request-Id header must not be duplicated when client sends call ID`() = testApplication {
    val clientCallId = "1234567890"

    routing {
      get("/") {
        call.respondText("ok")
      }
    }

    application {
      installCallId()
      installCallLogging()
    }

    startApplication()

    val response = client.get("/") {
      header(HttpHeaders.XRequestId, clientCallId)
    }

    val xRequestIdValues = response.headers.getAll(HttpHeaders.XRequestId)
    assertThat(xRequestIdValues).isNotNull()
    assertThat(xRequestIdValues!!).hasSize(1)
    assertThat(xRequestIdValues.single()).isEqualTo(clientCallId)
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.CallIdTest")
  }
}
