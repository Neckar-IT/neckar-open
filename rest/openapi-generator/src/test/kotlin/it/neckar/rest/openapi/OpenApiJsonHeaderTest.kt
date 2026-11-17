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
import assertk.assertions.*
import io.ktor.http.*
import it.neckar.open.http.UrlParameterName
import it.neckar.open.test.utils.isJsonEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.dsl.openApiRoute

/**
 *
 */
class OpenApiJsonHeaderTest {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Test
  fun testResponseHeader() {
    val route = openApiRoute("/foobars/{MyParam}/bars/{SecondParam}", HttpMethod.Get, emptyList()) {
      response("Test Response") {
        header("X-Test-Header", SchemaDescriptor.String) {
          example("exampleA", "abc123")
          description("A user defined header")
        }
        header("X-Test-Int-Header", SchemaDescriptor.Int) {
          example("exampleB", 7998)
          description("A user defined int header")
        }
        noExamples()
      }
    }

    assertThat(route.bestResponse?.headers).isNotNull().hasSize(2)


    assertThat(route.path.parameterNames()).hasSize(2)
    assertThat(route.path.parameterNames()).hasSize(2)
    assertThat(route.path.elements[1]).isInstanceOf<UrlParameterName>()
    assertThat(route.path.elements[3]).isInstanceOf<UrlParameterName>()

    val generateRouteDetails = generator.generateRouteDetails(route)

    assertThat(generateRouteDetails.toString()).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "operationId" : "GetBarForFoobar",
          "x-category" : "default",
          "parameters" : [ ],
          "responses" : {
            "200" : {
              "description" : "Test Response",
              "headers" : {
                "X-Test-Header" : {
                  "description" : "A user defined header",
                  "schema" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String"
                  },
                  "examples" : {
                    "exampleA" : {
                      "value" : "abc123"
                    }
                  }
                },
                "X-Test-Int-Header" : {
                  "description" : "A user defined int header",
                  "schema" : {
                    "type" : "integer",
                    "x-source-kotlin-type" : "kotlin.Int"
                  },
                  "examples" : {
                    "exampleB" : {
                      "value" : 7998
                    }
                  }
                }
              }
            }
          }
        }
      """.trimIndent()
    )
  }
}
