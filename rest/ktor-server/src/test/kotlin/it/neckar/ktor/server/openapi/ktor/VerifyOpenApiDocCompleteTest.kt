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
package it.neckar.ktor.server.openapi.ktor

import assertk.*
import assertk.assertions.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.collectRoutes
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.openapi.OpenApiException
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.UndocumentedRoute
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisableLogging
class VerifyOpenApiDocCompleteTest {
  @Test
  fun testMessageNamesEveryUndocumentedRoute() = testApplication {
    installOpenApiPluginForTest()
    installBasicAuthenticationForTest()

    routing {
      get("with-doc") {
      }.doc {
        summary = "A documented endpoint"
        description = "Returns nothing"
        noResponseBody()
      }

      route("api") {
        get("users/{id}") {
        }
        delete("users/{id}") {
        }
      }
      post("without-doc-post") {
      }

      authenticate("test-basic") {
        get("protected") {
        }
      }
    }

    startApplication()

    val exception = assertThrows<OpenApiException> {
      application.verifyOpenApiDocComplete()
    }

    assertThat(exception.message).isEqualTo(
      """
        Routes without open API documentation. Please add a 'doc{}' block to each:
          /(authenticate test-basic)/protected/(method:GET)
          /api/users/{id}/(method:DELETE)
          /api/users/{id}/(method:GET)
          /without-doc-post/(method:POST)
      """.trimIndent()
    )

    assertThat(exception.problem).isInstanceOf(OpenApiException.Problem.MissingDocumentation::class)
      .prop(OpenApiException.Problem.MissingDocumentation::routes).containsOnly(
      UndocumentedRoute("/(authenticate test-basic)/protected/(method:GET)"),
      UndocumentedRoute("/api/users/{id}/(method:GET)"),
      UndocumentedRoute("/api/users/{id}/(method:DELETE)"),
      UndocumentedRoute("/without-doc-post/(method:POST)"),
    )
  }

  @Test
  fun testPassesWhenEveryRouteIsDocumented() = testApplication {
    installOpenApiPluginForTest()
    installBasicAuthenticationForTest()

    routing {
      get("documented") {
      }.doc {
        summary = "A documented endpoint"
        description = "Returns nothing"
        noResponseBody()
      }

      authenticate("test-basic") {
        get("protected") {
        }.doc {
          summary = "A documented endpoint behind authentication"
          description = "Returns nothing"
          noResponseBody()
        }
      }
    }

    startApplication()

    assertThat(application.collectRoutes().map { it.toString() }).containsAll("/documented/(method:GET)", "/(authenticate test-basic)/protected/(method:GET)")
    application.verifyOpenApiDocComplete()
  }

  @Test
  fun testProblemNeedsARoute() {
    assertFailure {
      OpenApiException.Problem.MissingDocumentation(emptyList())
    }.isInstanceOf(IllegalArgumentException::class)
  }

  private fun ApplicationTestBuilder.installBasicAuthenticationForTest() {
    install(Authentication) {
      basic("test-basic") {
        validate { UserIdPrincipal(it.name) }
      }
    }
  }

  private fun ApplicationTestBuilder.installOpenApiPluginForTest() {
    install(OpenApiPlugin) {
      metadata = OpenApiMetaData(
        title = "Test API",
        description = "Da Test API",
      )
      authenticationRequirement = AuthenticationRequirement.None
      scalarUi = ScalarUiMode.Disabled
    }
  }
}
