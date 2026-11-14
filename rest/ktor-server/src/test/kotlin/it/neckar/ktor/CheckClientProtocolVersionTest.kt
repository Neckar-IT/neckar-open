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

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.version.VersionInformation
import it.neckar.rest.version.RestProtocolVersionInfo
import it.neckar.rest.version.RestProtocolVersionProblemException
import org.junit.jupiter.api.Test

/**
 * What the check does to a request, by the version the client states.
 */
@DisableLogging
class CheckClientProtocolVersionTest {

  @Test
  fun `a client of this build is answered`() = testApplication {
    serviceExpecting(ServerVersion)

    val response = client.get("/jobs") { header(RestProtocolVersionInfo.ProtocolVersionHeaderKey, ServerVersion) }

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `a client of another build is turned away`() = testApplication {
    serviceExpecting(ServerVersion)

    val response = client.get("/jobs") { header(RestProtocolVersionInfo.ProtocolVersionHeaderKey, "openapi:${"b".repeat(64)}") }

    assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
  }

  @Test
  fun `a client that states no version is answered`() = testApplication {
    serviceExpecting(ServerVersion)

    val response = client.get("/jobs")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(response.headers["${RestProtocolVersionInfo.ProtocolVersionHeaderKey}-Check-Skipped"])
      .isEqualTo("Version check skipped: the client states no version")
  }

  @Test
  fun `a client of another build is turned away on localhost too`() = testApplication {
    serviceExpecting(ServerVersion)

    val response = client.get("/jobs") {
      header(HttpHeaders.Host, "localhost")
      header(RestProtocolVersionInfo.ProtocolVersionHeaderKey, "openapi:${"b".repeat(64)}")
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
  }

  @Test
  fun `a client without injected deploy metadata is answered`() = testApplication {
    serviceExpecting(ServerVersion)

    val response = client.get("/jobs") {
      header(RestProtocolVersionInfo.ProtocolVersionHeaderKey, VersionInformation.UnknownValue)
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `a service that states no version of its own checks nothing`() = testApplication {
    serviceExpecting(VersionInformation.UnknownValue)

    val response = client.get("/jobs") { header(RestProtocolVersionInfo.ProtocolVersionHeaderKey, ServerVersion) }

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
  }

  /** One route behind the check, with the rejection rendered as the status the services answer with. */
  private fun ApplicationTestBuilder.serviceExpecting(expected: String) {
    application {
      install(CheckClientProtocolVersion) {
        expectedVersionString = expected
      }
      install(StatusPages) {
        exception<RestProtocolVersionProblemException> { call, _ -> call.respond(HttpStatusCode.BadRequest) }
      }
      routing {
        get("/jobs") { call.respondText("[]") }
      }
    }
  }

  companion object {
    private val ServerVersion: String = "openapi:" + "a".repeat(64)
  }
}
