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
import it.neckar.open.serialization.isJsonEqualTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiSealedWithoutTypeTest {

  private sealed interface FooInterface {
    @Serializable
    @SerialName("FooImpl")
    data class FooImpl(val bar: String) : FooInterface
  }

  @Test
  fun testType() {
    val schema = SchemaDescriptor.of<FooInterface>()
    //Represents a class schema object
    assertThat(schema).isInstanceOf(SchemaDescriptor.ClassSchemaObject::class)
  }

  @Test
  fun testDefaultBehavior() {
    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(
      listOf(
        SchemaDescriptor.of<FooInterface>()
      )
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiSealedWithoutTypeTest.FooInterface" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedWithoutTypeTest.FooInterface",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/OpenApiSealedWithoutTypeTest.FooInterface.FooImpl"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "FooImpl" : "#/components/schemas/OpenApiSealedWithoutTypeTest.FooInterface.FooImpl"
              }
            }
          },
          "OpenApiSealedWithoutTypeTest.FooInterface.FooImpl" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSealedWithoutTypeTest.FooInterface.FooImpl",
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
}
