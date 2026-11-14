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
import it.neckar.rest.RestResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import kotlin.reflect.full.starProjectedType
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.isClassSchemaObject
import it.neckar.rest.openapi.schema.isEnumSchema
import it.neckar.rest.openapi.schema.isInline
import it.neckar.rest.openapi.schema.isPrimitive
import it.neckar.rest.openapi.schema.isReferenced

/**
 *
 */
class OpenApiSchemaTypeReferenceTest {
  val openApiJsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  @Test
  fun testPrimitiveString() {
    buildJsonObject {
      openApiJsonGenerator.putSchemaTypeOrReference(SchemaDescriptor.String)
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
        """
          {
            "schema" : {
              "type" : "string",
              "x-source-kotlin-type" : "kotlin.String"
            }
          }
      """.trimIndent()
      )
    }
  }

  @Test
  fun testPrimitiveByteArray() {
    buildJsonObject {
      openApiJsonGenerator.putSchemaTypeOrReference(SchemaDescriptor.ByteArray)
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
        """
          {
            "schema" : {
              "type" : "string",
              "format" : "binary",
              "x-source-kotlin-type" : "kotlin.ByteArray"
            }
          }
        """.trimIndent()
      )
    }
  }

  /**
   * Probably never called in practice, but just to be sure
   */
  @Test
  fun testNothing() {
    buildJsonObject {
      openApiJsonGenerator.putSchemaTypeOrReference(SchemaDescriptor.EmptySchema)
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
        $$"""
            {
              "schema" : { }
            }
          """.trimIndent()
      )
    }
  }

  @Test
  fun testDataClass() {
    buildJsonObject {
      openApiJsonGenerator.putSchemaTypeOrReference(SchemaDescriptor.of(FooDataClass::class))
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
        $$"""
          {
            "schema" : {
              "$ref" : "#/components/schemas/OpenApiSchemaTypeReferenceTest.FooDataClass"
            }
          }
        """.trimIndent()
      )
    }
  }

  /**
   * Should not be called in practice, but just to be sure
   */
  @Test
  fun testValueClass() {
    buildJsonObject {
      openApiJsonGenerator.putSchemaTypeOrReference(SchemaDescriptor.of(FooValueClass::class))
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
        $$"""
          {
            "schema" : {
              "type" : "string",
              "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiSchemaTypeReferenceTest.FooValueClass"
            }
          }
        """.trimIndent()
      )
    }
  }

  @Test
  fun testFooResponse() {
    buildJsonObject {
      openApiJsonGenerator.putSchemaTypeOrReference(SchemaDescriptor.of(FooResponse::class))
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
        $$"""
          {
            "schema" : {
              "$ref" : "#/components/schemas/OpenApiSchemaTypeReferenceTest.FooResponse"
            }
          }
        """.trimIndent()
      )
    }
  }

  @Test
  fun testEnum() {
    buildJsonObject {
      openApiJsonGenerator.putSchemaTypeOrReference(SchemaDescriptor.of(FooEnum::class))
    }.let { jsonObject ->
      assertThat(jsonObject).isJsonEqualTo(
        //language=JSON
        $$"""
          {
            "schema" : {
              "$ref" : "#/components/schemas/OpenApiSchemaTypeReferenceTest.FooEnum"
            }
          }
        """.trimIndent()
      )
    }
  }

  @Test
  fun testOfPrimitive() {
    assertThat(SchemaDescriptor.of<String>()).all {
      isEqualTo(SchemaDescriptor.String)
      transform { it.isInline() }.isTrue()
      transform { it.isPrimitive() }.isTrue()
    }
  }

  @Test
  fun testOfEnum() {
    assertThat(SchemaDescriptor.of<FooEnum>()).all {
      isInstanceOf(SchemaDescriptor.EnumSchema::class)
      transform { it.isInline() }.isFalse()
      transform { it.isReferenced() }.isTrue()
      transform { it.isEnumSchema() }.isTrue()
      transform { it.asEnum().enumType }.isEqualTo(FooEnum::class.starProjectedType)
    }
  }

  @Test
  fun testOfDataClass() {
    assertThat(SchemaDescriptor.of<FooDataClass>()).all {
      isInstanceOf(SchemaDescriptor.ClassSchemaObject::class)
      transform { it.isInline() }.isFalse()
      transform { it.isReferenced() }.isTrue()
      transform { it.isEnumSchema() }.isFalse()
      transform { it.isClassSchemaObject() }.isTrue()
      transform { it.asClassSchemaObject().type }.isEqualTo(FooDataClass::class.starProjectedType)
    }
  }

  private enum class FooEnum {
    A, B, C
  }

  @Serializable
  private data class FooDataClass(
    val foo: String,
  )

  @JvmInline
  private value class FooValueClass(val foo: String)

  /**
   * Response for
   */
  @Serializable
  private sealed interface FooResponse : RestResponse<FooDataClass> {
    @Serializable
    @SerialName("bad/Failed")
    private data object Failed : FooResponse, RestResponse.BadResponse<FooDataClass>
  }
}
