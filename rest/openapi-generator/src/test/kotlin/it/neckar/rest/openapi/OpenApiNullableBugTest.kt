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
import it.neckar.open.test.utils.isJsonEqualTo
import it.neckar.problem.ExpectedApplicationError
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiNullableBugTest {
  @Test
  fun testExpectedApplicationError() {
    assertThat(
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(listOf(SchemaDescriptor.of(ExpectedApplicationError::class)))
        .toString()
    ).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "ExpectedApplicationError.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.problem.ExpectedApplicationError",
            "allOf" : [ {
              "$ref" : "#/components/schemas/ExpectedApplicationError"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "ExpectedError" ]
              }
            },
            "required" : [ "type" ]
          },
          "ExpectedApplicationError" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.problem.ExpectedApplicationError",
            "properties" : {
              "errorCode" : {
                "type" : "string"
              },
              "fallbackMessage" : {
                "type" : [ "string", "null" ]
              },
              "instanceUri" : {
                "type" : [ "string", "null" ]
              },
              "attributes" : {
                "type" : [ "object", "null" ],
                "additionalProperties" : true
              },
              "traceId" : {
                "x-source-kotlin-type" : "it.neckar.open.tracing.TraceId",
                "type" : [ "string", "null" ]
              }
            },
            "required" : [ "errorCode" ]
          }
        }
      """.trimIndent()
    )
  }
}
