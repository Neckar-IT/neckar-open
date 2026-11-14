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
import it.neckar.open.serialization.isJsonEqualTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiRefToUntypedTest {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Test
  fun testIt() {
    val json = generator.generateComponentsSchemas(
      listOf(
        SchemaDescriptor.of<DataClass>(),
      )
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiRefToUntypedTest.DataClass" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRefToUntypedTest.DataClass",
            "properties" : {
              "implA" : {
                "$ref" : "#/components/schemas/OpenApiRefToUntypedTest.MySealedInterface.ImplA"
              },
              "sealedInterface" : {
                "$ref" : "#/components/schemas/OpenApiRefToUntypedTest.MySealedInterface"
              }
            },
            "required" : [ "implA", "sealedInterface" ]
          },
          "OpenApiRefToUntypedTest.MySealedInterface" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRefToUntypedTest.MySealedInterface",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/OpenApiRefToUntypedTest.MySealedInterface.ImplA.WithType"
            }, {
              "$ref" : "#/components/schemas/OpenApiRefToUntypedTest.MySealedInterface.ImplB.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "ImplA" : "#/components/schemas/OpenApiRefToUntypedTest.MySealedInterface.ImplA.WithType",
                "ImplB" : "#/components/schemas/OpenApiRefToUntypedTest.MySealedInterface.ImplB.WithType"
              }
            }
          },
          "OpenApiRefToUntypedTest.MySealedInterface.ImplB.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRefToUntypedTest.MySealedInterface.ImplB",
            "allOf" : [ {
              "$ref" : "#/components/schemas/OpenApiRefToUntypedTest.MySealedInterface.ImplB"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "ImplB" ]
              }
            },
            "required" : [ "type" ]
          },
          "OpenApiRefToUntypedTest.MySealedInterface.ImplB" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRefToUntypedTest.MySealedInterface.ImplB",
            "properties" : {
              "number" : {
                "type" : "integer"
              }
            },
            "required" : [ "number" ]
          },
          "OpenApiRefToUntypedTest.MySealedInterface.ImplA.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRefToUntypedTest.MySealedInterface.ImplA",
            "allOf" : [ {
              "$ref" : "#/components/schemas/OpenApiRefToUntypedTest.MySealedInterface.ImplA"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "ImplA" ]
              }
            },
            "required" : [ "type" ]
          },
          "OpenApiRefToUntypedTest.MySealedInterface.ImplA" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiRefToUntypedTest.MySealedInterface.ImplA",
            "properties" : {
              "data" : {
                "type" : "string"
              }
            },
            "required" : [ "data" ]
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class DataClass(
    val implA: MySealedInterface.ImplA,
    val sealedInterface: MySealedInterface,
  )

  @Serializable
  private sealed interface MySealedInterface {
    @Serializable
    @SerialName("ImplA")
    data class ImplA(val data: String) : MySealedInterface

    @Serializable
    @SerialName("ImplB")
    data class ImplB(val number: Int) : MySealedInterface
  }
}
