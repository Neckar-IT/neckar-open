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

import assertk.*
import io.ktor.http.*
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.RestResponse
import it.neckar.rest.RestResponseFilteredSorted
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.openApiRoute

/**
 *
 */
class OpenApiFilterVoidTest {
  @Test
  fun testResponseWithVoidFilter() {
    val route = openApiRoute("/daPath", method = HttpMethod.Get, pathParameterNames = emptyList()) {
      description = "A test route"
      summary = "A test route summary"

      responding<FooResponse> {
        noExamples()
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateRouteDetails(route)

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "description" : "A test route",
          "summary" : "A test route summary",
          "operationId" : "GetDaPath",
          "x-category" : "default",
          "parameters" : [ ],
          "responses" : {
            "200" : {
              "description" : "Responds with Good or Bad types",
              "content" : {
                "application/json" : {
                  "schema" : {
                    "$ref" : "#/components/schemas/OpenApiFilterVoidTest.FooResponse"
                  }
                }
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class Foo(val bar: String)

  @Serializable
  private sealed interface FooResponse : RestResponseFilteredSorted<Foo, Nothing, Nothing> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: Foo) : FooResponse, RestResponse.GoodResponse<Foo>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : FooResponse, RestResponse.BadResponse<Foo>

    companion object {
      /**
       * Helper method to create a success object - with the correct type
       */
      fun good(data: Foo): FooResponse = Good(data)

      /**
       * Helper method to create a failure object - with the correct type
       */
      fun bad(): FooResponse = Failed
    }
  }
}
