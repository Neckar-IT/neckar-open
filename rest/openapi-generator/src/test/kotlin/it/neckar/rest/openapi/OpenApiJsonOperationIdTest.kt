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
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OperationId
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.dsl.openApiRoute

/**
 *
 */
class OpenApiJsonOperationIdTest {

  @Test
  fun testOpenApi() {
    val route = openApiRoute("/foobar", HttpMethod.Put, emptyList()) {
      operationId = OperationId("FoobarPut")
      request {
        body<Nothing>(SchemaDescriptor.EmptySchema) {
          noExamples()
        }
      }
      noResponseBody()
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(
      listOf(route),
      additionalTypes = emptyList(),
      metaData = OpenApiMetaData.default,
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "openapi" : "3.1.0",
          "info" : {
            "title" : "REST API",
            "description" : "REST API",
            "version" : "1.0.0"
          },
          "servers" : [ ],
          "paths" : {
            "/foobar" : {
              "put" : {
                "operationId" : "FoobarPut",
                "x-category" : "default",
                "parameters" : [ ],
                "responses" : {
                  "204" : {
                    "description" : "Successful request with no content"
                  }
                }
              }
            }
          },
          "security" : [ {
    "bearerAuth" : [ ]
  } ],
  "components" : {
            "securitySchemes" : {
              "bearerAuth" : {
                "type" : "http",
                "scheme" : "bearer",
                "bearerFormat" : "JWT"
              }
            },
            "schemas" : { }
          }
        }
      """.trimIndent()
    )
  }
}
