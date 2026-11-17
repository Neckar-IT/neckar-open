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
package it.neckar.ktor.authorization

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import it.neckar.ktor.collectPathSegments
import it.neckar.ktor.collectSelectors
import it.neckar.ktor.installJson
import it.neckar.ktor.status.installStatusPages
import it.neckar.open.authorization.Capability
import it.neckar.open.authorization.CapabilityKey
import it.neckar.open.authorization.UserCapabilities
import it.neckar.open.http.UrlPathSegments
import it.neckar.open.test.utils.DisableLogging
import org.junit.jupiter.api.Test

/**
 * The guard states the capability at the route instead of inside the handler. These tests pin what
 * that placement must not change: an unauthenticated call keeps its 401, and the path of a guarded
 * route stays what it was — the OpenAPI document is built from the route tree the guard nests into.
 */
@DisableLogging
class RouteAuthorizationTest {

  @Test
  fun `a caller holding the capability reaches the handler`() = testApplication {
    application {
      installGate(UserCapabilities(setOf(DeleteBooks.key)))

      routing {
        authorize(DeleteBooks) {
          get("/books") { call.respondText("the books") }
        }
      }
    }

    val response = client.get("/books")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(response.bodyAsText()).isEqualTo("the books")
  }

  @Test
  fun `a caller without the capability is answered 403 naming what is missing`() = testApplication {
    application {
      installGate(UserCapabilities.Empty)

      routing {
        authorize(DeleteBooks) {
          get("/books") { call.respondText("the books") }
        }
      }
    }

    val response = client.get("/books")

    assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
    assertThat(response.bodyAsText()).contains("demo:delete-books")
  }

  @Test
  fun `a call without credentials stays a 401 instead of turning into a 403`() = testApplication {
    application {
      installGate(UserCapabilities.Empty)
      authentication { basicAuthRejectingEveryone() }

      routing {
        authenticate("demo") {
          authorize(DeleteBooks) {
            get("/books") { call.respondText("the books") }
          }
        }
      }
    }

    val response = client.get("/books")

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test
  fun `the guard contributes no path segment`() = testApplication {
    lateinit var guardedPath: UrlPathSegments
    lateinit var plainPath: UrlPathSegments

    application {
      installGate(UserCapabilities(setOf(DeleteBooks.key)))

      routing {
        route("api") {
          authorize(DeleteBooks) {
            guardedPath = get("books") { call.respondText("the books") }.collectSelectors().collectPathSegments()
          }
        }
        route("plain") {
          plainPath = get("books") { call.respondText("the books") }.collectSelectors().collectPathSegments()
        }
      }
    }

    client.get("/api/books")

    assertThat(guardedPath.elements.map { it.toString() }).isEqualTo(listOf("api", "books"))
    assertThat(guardedPath.elements.size).isEqualTo(plainPath.elements.size)
  }

  private fun Application.installGate(capabilities: UserCapabilities) {
    installStatusPages(logger = mockk(relaxed = true))
    installJson { }
    installCapabilityChecks { capabilities }
  }

  private fun AuthenticationConfig.basicAuthRejectingEveryone() {
    basic("demo") {
      validate { null }
    }
  }

  private data object DeleteBooks : Capability {
    override val key: CapabilityKey = CapabilityKey("demo:delete-books")
  }
}
