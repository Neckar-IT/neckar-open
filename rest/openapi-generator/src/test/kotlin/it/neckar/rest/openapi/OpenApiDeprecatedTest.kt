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
import assertk.assertions.isEqualTo
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.neckar.lifeCycle.ArchiveState
import it.neckar.open.http.UrlParameterName
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.openApiRoute
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

/**
 * An archived route or parameter ([ArchiveState.Archived]) is exposed as `deprecated: true`.
 * A parameter's examples are written as an OpenAPI `examples` object.
 */
class OpenApiDeprecatedTest {
  private val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  @Test
  fun `an active route is not deprecated`() {
    val route = openApiRoute("/current", HttpMethod.Get, emptyList()) {
      response("ok") {
        statusCode = HttpStatusCode.OK
        noExamples()
      }
    }
    val details = generator.generateRouteDetails(route).jsonObject
    assertThat(details.containsKey("deprecated")).isEqualTo(false)
  }

  @Test
  fun `an archived route is deprecated`() {
    val route = openApiRoute("/legacy", HttpMethod.Get, emptyList()) {
      archiveState = ArchiveState.Archived
      response("ok") {
        statusCode = HttpStatusCode.OK
        noExamples()
      }
    }
    val details = generator.generateRouteDetails(route).jsonObject
    assertThat(details.getValue("deprecated").jsonPrimitive.boolean).isEqualTo(true)
  }

  @Test
  fun `an archived parameter is deprecated and its examples are emitted`() {
    val route = openApiRoute("/items", HttpMethod.Get, emptyList()) {
      request {
        parameterString(UrlParameterName("status")) {
          description = "filter by status"
          archiveState(ArchiveState.Archived)
          example("active", "active")
        }
      }
      response("ok") {
        statusCode = HttpStatusCode.OK
        noExamples()
      }
    }

    val parameter = generator.generateRouteDetails(route).jsonObject
      .getValue("parameters").jsonArray.first().jsonObject

    assertThat(parameter.getValue("deprecated").jsonPrimitive.boolean).isEqualTo(true)
    assertThat(parameter.getValue("examples")).isJsonEqualTo(
      //language=JSON
      """{ "active": { "value": "active" } }"""
    )
  }
}
