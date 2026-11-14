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
package it.neckar.rest.openapi

import assertk.assertThat
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.openApiRoute
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

/**
 * A route can declare a `default` (catch-all) response next to its status-code responses.
 */
class OpenApiDefaultResponseTest {
  private val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  @Test
  fun `a default response is emitted under the default key next to status codes`() {
    val route = openApiRoute("/things", HttpMethod.Get, emptyList()) {
      response("ok") {
        statusCode = HttpStatusCode.OK
        noExamples()
      }
      defaultResponse("Unexpected error") {
        noExamples()
      }
    }

    val responses = generator.generateRouteDetails(route).jsonObject.getValue("responses")

    assertThat(responses).isJsonEqualTo(
      //language=JSON
      """
      {
        "200": { "description": "ok" },
        "default": { "description": "Unexpected error" }
      }
      """.trimIndent()
    )
  }
}
