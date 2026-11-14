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
import it.neckar.geometry.Rectangle
import it.neckar.kotlin.reflect.isNativeType
import it.neckar.open.http.Url
import it.neckar.open.kotlin.lang.isInterface
import it.neckar.open.kotlin.lang.isNotNothing
import it.neckar.open.kotlin.lang.isNothing
import it.neckar.open.kotlin.lang.isObject
import it.neckar.open.kotlin.lang.isSealed
import it.neckar.open.kotlin.lang.isValueClass
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.kotlin.reflect.fqNameToClassForName
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.rest.RestResponse
import it.neckar.user.UserLoginName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.dsl.openApiRoute

/**
 *
 */
class OpenApiJsonGeneratorTest {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Test
  fun testBugIsInterface() {
    assertThat(MyFooBarDataClass::class.isData).isTrue()
    assertThat(MyFooBarDataClass::class.isAbstract).isFalse()
    assertThat(MyFooBarDataClass::class.isCompanion).isFalse()
    assertThat(MyFooBarDataClass::class.isInterface).isFalse()

    val type: KType = MyFooBarDataClass::class.starProjectedType
    assertThat(type.isSealed()).isFalse()
    assertThat(type.isInterface()).isFalse()
    assertThat(type.isObject()).isFalse()
    assertThat(type.isNativeType()).isFalse()
    assertThat(type.isValueClass()).isFalse()
    assertThat(type.isNothing()).isFalse()
    assertThat(type.isNotNothing()).isTrue()
  }

  @Test
  fun testSerializeShapeSubclasses() {
    val calledFor = mutableListOf<SchemaDescriptor>()

    val json = buildJsonObject {
      generator.buildSchemaType(SchemaDescriptor.referenceObject(Rectangle::class)) {
        calledFor.add(it)
      }
    }

    assertThat(json.toStringPretty()).isEqualTo(
      //language=JSON
      $$"""
        {
          "Rectangle": {
            "type": "object",
            "x-source-kotlin-type": "it.neckar.geometry.Rectangle",
            "properties": {
              "location": {
                "$ref": "#/components/schemas/Coordinates"
              },
              "size": {
                "$ref": "#/components/schemas/Size"
              }
            },
            "required": [
              "location",
              "size"
            ]
          }
        }
    """.trimIndent()
    )

    //Contains Coordinates + Size
    assertThat(calledFor).hasSize(2)
  }

  @Test
  fun testClassForName() {
    assertThat(MyFooObject::class.qualifiedName).isEqualTo("it.neckar.rest.openapi.OpenApiJsonGeneratorTest.MyFooObject")

    Class.forName("it.neckar.rest.openapi.OpenApiJsonGeneratorTest\$MyFooObject")
    Class.forName(fqNameToClassForName(MyFooObject::class.qualifiedName.requireNotNull()))
  }

  @Test
  fun testDefaults() {
    val route = openApiRoute("/foobar", HttpMethod.Get, emptyList()) {
      noResponseBody()
    }

    assertThat(route.bestResponse?.body).isNull()

    val json = generator.generatePathEntry(listOf(route))
    assertThat(json).isJsonEqualTo(
      //language=JSON
      """
        {
          "get" : {
            "x-category" : "default",
            "operationId" : "GetFoobar",
            "parameters" : [ ],
            "responses" : {
              "204" : {
                "description" : "Successful request with no content"
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testBuildRefTypeForDataClass() {
    val json = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildSchemaTypeForDataClass(typeOf<MyFooBarDataClass>()) {
        //ignore
      }
    }

    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "object",
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorTest.MyFooBarDataClass",
          "properties" : {
            "foo" : {
              "type" : "string"
            },
            "bar" : {
              "type" : "integer"
            },
            "elements" : {
              "type" : "array",
              "items" : {
                "$ref" : "#/components/schemas/OpenApiJsonGeneratorTest.MyFooObject"
              }
            },
            "anOptionalBool" : {
              "type" : "boolean"
            }
          },
          "required" : [ "foo", "bar", "elements" ]
        }
      """.trimIndent()
    )
  }

  /**
   * URL is serialized inline
   */
  @Test
  fun testUrl() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    assertThat(generator.generateSchemaTypeRef(SchemaDescriptor.of(Url::class.starProjectedType), listOf())).isOpenApiEqualTo(
      //language=JSON
      """
        {
          "schema" : {
            "type" : "string",
            "format" : "uri",
            "x-source-kotlin-type" : "it.neckar.open.http.Url"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testSchemaBoolean() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    assertThat(generator.generateSchemaTypeRef(SchemaDescriptor.Boolean, listOf())).isOpenApiEqualTo(
      //language=JSON
      """
        {
          "schema" : {
            "type" : "boolean",
            "x-source-kotlin-type" : "kotlin.Boolean"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testSchemaString() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    assertThat(generator.generateSchemaTypeRef(SchemaDescriptor.String, listOf())).isOpenApiEqualTo(
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

  @Test
  fun testFooBar() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val schemaType = SchemaDescriptor.of<FooBar>()

    assertThat(generator.generateSchemaTypeRef(schemaType, listOf())).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "schema" : {
           "$ref" : "#/components/schemas/OpenApiJsonGeneratorTest.FooBar"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testFooBarResponseSuccessRefType() {
    val referencedTypes = mutableListOf<SchemaDescriptor>()

    val json = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildSchemaTypeUsingSerialDescriptor(typeOf<FooBarResponse.Good>()) { it: SchemaDescriptor ->
        referencedTypes.add(it)
      }
    }

    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "object",
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorTest.FooBarResponse.Good",
          "properties" : {
            "data" : {
              "$ref" : "#/components/schemas/OpenApiJsonGeneratorTest.FooBar"
            }
          },
          "required" : [ "data" ]
        }
      """.trimIndent()
    )

    assertThat(referencedTypes).hasSize(1)
    assertThat(referencedTypes).contains(SchemaDescriptor.of<FooBar>())
  }

  @Test
  fun testFooBarResponseFailureComplicatedInstanceRefType() {
    val json = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildSchemaTypeUsingSerialDescriptor(typeOf<FooBarComplicatedResponse.ValidationError>()) { it: SchemaDescriptor ->
      }
    }

    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "object",
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorTest.FooBarComplicatedResponse.ValidationError",
          "properties" : {
            "validationErrorType" : {
              "$ref" : "#/components/schemas/OpenApiJsonGeneratorTest.FooBarComplicatedResponse.ValidationErrorType"
            }
          },
          "required" : [ "validationErrorType" ]
        }
      """.trimIndent()
    )
  }

  @Test
  fun testObject() {
    val serializer = MyFooObject.serializer()
    assertThat(serializer).isNotNull()
    assertThat(serializer.descriptor.elementsCount).isEqualTo(0) //since it is an object!

    val json = with(generator) {
      buildJsonObject {
        buildSchemaTypeForObject(MyFooObject::class, RequiredPropertiesMode.AllPropertiesWithValue) {
          throw UnsupportedOperationException("Should not be called for $it")
        }
      }
    }

    //
    // When serializing an object, no properties are added since it is not possible to instantiate an object.
    //
    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      """
        {
          "type" : "object",
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorTest.MyFooObject",
          "required" : [ "Foo", "elements", "elementsEmpty", "url" ],
          "properties" : {
            "elements" : {
              "type" : "string",
              "x-source-kotlin-type" : "kotlin.collections.List<it.neckar.rest.openapi.OpenApiJsonGeneratorTest.MyFooBarDataClass>",
              "const" : [ "MyFooBarDataClass(foo=foo, bar=42, elements=[], anOptionalBool=true)" ]
            },
            "elementsEmpty" : {
              "type" : "string",
              "x-source-kotlin-type" : "kotlin.collections.List<it.neckar.rest.openapi.OpenApiJsonGeneratorTest.MyFooBarDataClass>",
              "const" : [ ]
            },
            "Foo" : {
              "type" : "string",
              "x-source-kotlin-type" : "kotlin.String",
              "const" : "foo"
            },
            "url" : {
              "type" : "string",
              "format" : "uri",
              "x-source-kotlin-type" : "it.neckar.open.http.Url",
              "const" : "asdf"
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testClassRefTypeWithInlineClass() {
    val json = with(generator) {
      buildJsonObject {
        buildSchemaTypeForClass(typeOf<MyFooBarWithUser>()) {
          throw UnsupportedOperationException("Should not be called for $it")
        }
      }
    }

    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      """
        {
          "type" : "object",
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorTest.MyFooBarWithUser",
          "properties" : {
            "foo" : {
              "type" : "string"
            },
            "user" : {
              "type" : "string"
            }
          },
          "required" : [ "foo", "user" ]
        }
      """.trimIndent()
    )
  }

  @Test
  fun testFooBarResponse() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
    val schemaType = SchemaDescriptor.of<FooBarResponse>()

    assertThat(generator.generateSchemaTypeRef(schemaType, listOf())).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "schema" : {
            "$ref" : "#/components/schemas/OpenApiJsonGeneratorTest.FooBarResponse"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testSourceRef() {
    val openapiJsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    assertThat(
      buildJsonObject {
        openapiJsonGenerator.putKotlinSourceRef(SchemaDescriptor.Double)
      }
    ).isJsonEqualTo(
      """
        {
          "x-source-kotlin-type" : "kotlin.Double"
        }
      """.trimIndent()
    )

    assertThat(
      buildJsonObject {
        openapiJsonGenerator.putKotlinSourceRef(SchemaDescriptor.of<FooBarResponse>())
      }
    ).isJsonEqualTo(
      """
        {
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorTest.FooBarResponse"
        }
      """.trimIndent()
    )

    assertThat(
      buildJsonObject {
        openapiJsonGenerator.putKotlinSourceRef(SchemaDescriptor.of<FooBar>())
      }
    ).isJsonEqualTo(
      """
        {
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorTest.FooBar"
        }
      """.trimIndent()
    )

    assertThat(
      buildJsonObject {
        openapiJsonGenerator.putKotlinSourceRef(SchemaDescriptor.of<FooBarEnum>())
      }
    ).isJsonEqualTo(
      """
        {
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorTest.FooBarEnum"
        }
      """.trimIndent()
    )

    assertThat(
      buildJsonObject {
        openapiJsonGenerator.putKotlinSourceRef(SchemaDescriptor.enumEntry(FooBarEnum.Val1))
      }
    ).isJsonEqualTo(
      """
        {
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorTest.FooBarEnum.Val1"
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class FooBar(
    val foo: String,
    val bar: Int = 42,
  )

  @Serializable
  private enum class FooBarEnum(val foo: String) {
    Val1("the-val-1"),
    Val2("the-val-2"),
  }

  /**
   * Response for FooBar
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

  /**
   * Response for
   */
  @Serializable
  private sealed interface FooBarComplicatedResponse : RestResponse<FooBar> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: FooBar) : FooBarComplicatedResponse, RestResponse.GoodResponse<FooBar>

    /**
     * Represents a failure response
     */
    @Serializable
    private sealed interface Failed : FooBarComplicatedResponse, RestResponse.BadResponse<FooBar>

    @Serializable
    @SerialName("bad/NotFound")
    data object NotFound : Failed

    /**
     * Is only returned on PUT if the validation failed on the server.
     */
    @Serializable
    @SerialName("bad/ValidationError")
    data class ValidationError(
      val validationErrorType: ValidationErrorType,
    ) : Failed

    private enum class ValidationErrorType {
      ValidationErr1,
      ValidationErr2,
    }
  }

  @Serializable
  internal data class MyFooBarDataClass(
    val foo: String,
    val bar: Int,
    val elements: List<MyFooObject>,
    val anOptionalBool: Boolean = true,
  )

  @Serializable
  internal object MyFooObject {
    const val Foo: String = "foo"

    val url: Url = Url.relative("asdf")

    val elementsEmpty: List<MyFooBarDataClass> = emptyList()

    val elements: List<MyFooBarDataClass> = listOf(
      MyFooBarDataClass(
        foo = "foo",
        bar = 42,
        elements = emptyList(),
      )
    )

    fun myFunction() {
      // Intentionally empty — fixture for serialization descriptor tests
    }
  }

  @Serializable
  private class MyFooBarWithUser(
    val foo: String,
    val user: UserLoginName,
  ) {

    fun myFunction() {
      // Intentionally empty — fixture for serialization descriptor tests
    }
  }
}
