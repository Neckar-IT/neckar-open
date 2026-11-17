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
import it.neckar.open.kotlin.lang.getSimpleTypeName
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.RestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.isConstSchemaObject
import it.neckar.rest.openapi.schema.isDiscriminatedClassSchema

/**
 *
 */
class OpenApiBuildSchemaTypeEntryTest {
  @Test
  fun testClass() {
    val json = buildJsonObject {
      val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
      generator.buildSchemaType(SchemaDescriptor.referenceObject(FooBar::class)) {
        fail("Should not be called for $it")
      }
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
          {
            "OpenApiBuildSchemaTypeEntryTest.FooBar" : {
              "type" : "object",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiBuildSchemaTypeEntryTest.FooBar",
              "properties" : {
                "bar" : {
                  "type" : "string"
                }
              },
              "required" : [ "bar" ]
            }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testSealedResponse() {
    val called = mutableListOf<SchemaDescriptor>()

    val json = buildJsonObject {
      val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
      generator.buildSchemaType(SchemaDescriptor.referenceObject(FooBarResponse::class)) {
        called.add(it)
      }
    }

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiBuildSchemaTypeEntryTest.FooBarResponse" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiBuildSchemaTypeEntryTest.FooBarResponse",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/OpenApiBuildSchemaTypeEntryTest.FooBarResponse.Failed"
            }, {
              "$ref" : "#/components/schemas/OpenApiBuildSchemaTypeEntryTest.FooBarResponse.Good.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "bad/Failed" : "#/components/schemas/OpenApiBuildSchemaTypeEntryTest.FooBarResponse.Failed",
                "good" : "#/components/schemas/OpenApiBuildSchemaTypeEntryTest.FooBarResponse.Good.WithType"
              }
            }
          }
        }
      """.trimIndent()
    )

    assertThat(called).hasSize(2)
    assertThat(called[0].isConstSchemaObject()).isTrue()
    assertThat(called[1].isDiscriminatedClassSchema()).isTrue()

    assertThat((called[0].asConstSchemaObject()).type.getSimpleTypeName()).isEqualTo("OpenApiBuildSchemaTypeEntryTest.FooBarResponse.Failed")
    assertThat((called[1].asClassSchemaObject()).getSimpleTypeName()).isEqualTo("OpenApiBuildSchemaTypeEntryTest.FooBarResponse.Good")
  }

  @Serializable
  private data class FooBar(
    val bar: String
  )

  /**
   * Response for
   */
  @Serializable
  private sealed interface FooBarResponse : RestResponse<FooBar> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: FooBar) : FooBarResponse, RestResponse.GoodResponse<FooBar>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : FooBarResponse, RestResponse.BadResponse<FooBar>

    companion object {
      /**
       * Helper method to create a success object - with the correct type
       */
      fun good(data: FooBar): FooBarResponse = Good(data)

      /**
       * Helper method to create a failure object - with the correct type
       */
      fun bad(): FooBarResponse = Failed

      /**
       * Creates a success method for non-null results, failure for null results
       */
      fun createFor(data: FooBar?): FooBarResponse {
        return if (data != null) {
          good(data)
        } else {
          bad()
        }
      }
    }
  }

}
