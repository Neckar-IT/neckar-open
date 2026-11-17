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
import it.neckar.open.serialization.roundTrip
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.schema.SchemaDescriptor.ComponentSchema.ReferenceType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.isConstSchemaObject

/**
 *
 */
class ObjectSchemaTest {
  @Test
  fun testOpenApiObjectSchemaRef() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val schemaType = SchemaDescriptor.of(FooBarObject::class)
    assertThat(schemaType.isConstSchemaObject()).isTrue()
    assertThat(schemaType.asConstSchemaObject().reference(ReferenceType.Discriminated).unescapedValue).isEqualTo("ObjectSchemaTest.FooBarObject")
    assertThat(schemaType.asConstSchemaObject().reference(ReferenceType.Discriminated).completeReference()).isEqualTo("#/components/schemas/ObjectSchemaTest.FooBarObject")

    val json = jsonGenerator.generateSchemaTypeRef(schemaType, emptyList())
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
            {
              "schema" : {
                "$ref" : "#/components/schemas/ObjectSchemaTest.FooBarObject"
              }
            }
      """.trimIndent()
    )
  }

  @Test
  fun testOpenApiMessageObjectSchema() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val json = jsonGenerator.generateComponentsSchemas(listOf(SchemaDescriptor.message(FooBarObject::class)))
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "ObjectSchemaTest.FooBarObject" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.ObjectSchemaTest.FooBarObject",
            "properties" : {
              "bar" : {
                "type" : "integer",
                "x-source-kotlin-type" : "kotlin.Int",
                "const" : 17
              },
              "foo" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "foo"
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testOpenApiConstantCatalogSchema() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val json = jsonGenerator.generateComponentsSchemas(listOf(SchemaDescriptor.of(FooBarObject::class)))
    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "ObjectSchemaTest.FooBarObject" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.ObjectSchemaTest.FooBarObject",
            "required" : [ "bar", "foo" ],
            "properties" : {
              "bar" : {
                "type" : "integer",
                "x-source-kotlin-type" : "kotlin.Int",
                "const" : 17
              },
              "foo" : {
                "type" : "string",
                "x-source-kotlin-type" : "kotlin.String",
                "const" : "foo"
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testComplete() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val generated = jsonGenerator.generate(emptyList(), listOf(SchemaDescriptor.of(FooBarObject::class)), OpenApiMetaData.default)

    assertThat(generated).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "openapi" : "3.1.0",
          "info" : {
            "title" : "REST API",
            "description" : "REST API",
            "version" : "1.0.0"
          },
          "servers" : [ ],
          "paths" : { },
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
            "schemas" : {
              "ObjectSchemaTest.FooBarObject" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.ObjectSchemaTest.FooBarObject",
                "required" : [ "bar", "foo" ],
                "properties" : {
                  "bar" : {
                    "type" : "integer",
                    "x-source-kotlin-type" : "kotlin.Int",
                    "const" : 17
                  },
                  "foo" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String",
                    "const" : "foo"
                  }
                }
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testSealedVariantObjectRequiresOnlyDiscriminator() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val json = jsonGenerator.generateComponentsSchemas(listOf(SchemaDescriptor.of(Outcome::class)))

    val doneSchema = json.getValue("ObjectSchemaTest.Outcome.Done").jsonObject
    assertThat(doneSchema.getValue("required").jsonArray.map { it.jsonPrimitive.content }).containsExactly("type")
    assertThat(doneSchema.getValue("properties").jsonObject.keys).containsExactlyInAnyOrder("type", "code")
  }

  @Test
  fun testMessageWinsOverConstantCatalogInEveryOrder() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    listOf(
      listOf(SchemaDescriptor.message(FooBarObject::class), SchemaDescriptor.const(FooBarObject::class)),
      listOf(SchemaDescriptor.const(FooBarObject::class), SchemaDescriptor.message(FooBarObject::class)),
    ).forEach { descriptors ->
      val json = jsonGenerator.generateComponentsSchemas(descriptors)
      assertThat(json.getValue("ObjectSchemaTest.FooBarObject").jsonObject.containsKey("required")).isFalse()
    }
  }

  @Test
  fun testConstantCatalogWithoutPropertiesHasNoRequired() {
    val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val json = jsonGenerator.generateComponentsSchemas(listOf(SchemaDescriptor.const(EmptyObject::class)))
    assertThat(json.getValue("ObjectSchemaTest.EmptyObject").jsonObject.containsKey("required")).isFalse()
  }

  @Test
  fun testProperties() {
    val memberProperties = FooBarObject::class.memberProperties.toList()
    assertThat(memberProperties).hasSize(2)

    memberProperties.first { it.name == "foo" }.let {
      assertThat(it.name).isEqualTo("foo")
      assertThat(it.returnType.isMarkedNullable).isFalse()
      assertThat(it.returnType.classifier).isEqualTo(String::class)
      assertThat(it.getter.invoke(FooBarObject)).isEqualTo("foo")
      assertThat(it.get(FooBarObject)).isEqualTo("foo")
    }

    memberProperties.first { it.name == "bar" }.let {
      assertThat(it.name).isEqualTo("bar")
      assertThat(it.returnType.isMarkedNullable).isFalse()
      assertThat(it.returnType.classifier).isEqualTo(Int::class)
      assertThat(it.get(FooBarObject)).isEqualTo(17)
    }
  }

  @Test
  fun testSchemaType() {
    assertThat(SchemaDescriptor.of(FooBarObject::class)).isEqualTo(SchemaDescriptor.of<FooBarObject>())
    val klass = FooBarObject::class
    assertThat(klass.objectInstance).isSameInstanceAs(FooBarObject)
  }

  @Test
  fun testTypes() {
    val kClass = FooBarObject::class
    val property = kClass.memberProperties.toList().first { it.name == "bar" }

    assertThat(property.get(FooBarObject)).isEqualTo(17)

    ////
    //Wildcard type
    ////

    val wildCard = kClass as KClass<*>
    val wildProperty = wildCard.memberProperties.toList()[0] as KProperty1<Any, *>

    assertThat(wildProperty.get(FooBarObject)).isEqualTo(17)
  }

  @Test
  fun testJson() {
    roundTrip(
      FooBarObject,
    ) {
      "{}".trimIndent()
    }
  }

  @Suppress("MayBeConstant")
  @Serializable
  internal object FooBarObject {
    val foo: String = "foo"
    val bar = 17

    //TODO support later!
    //val names = listOf("a", "b", "c")
  }

  @Serializable
  object EmptyObject

  @Serializable
  sealed interface Outcome {
    @Serializable
    @SerialName("done")
    object Done : Outcome {
      val code: Int = 7
    }
  }
}
