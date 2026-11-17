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
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.rest.openapi.OpenApiPropertyGenerator.NamedPropertyCreationContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.internal.GeneratedSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import it.neckar.open.test.utils.isOpenApiEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.SchemaPropertyConstraints

class OpenApiJsonGeneratorSealedTest {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Test
  fun testSerializer() {
    val serializer = FooWithPropOfSealedSubclass.serializer()
    serializer.descriptor.let { descriptor ->
      assertThat(descriptor).isNotNull()
      assertThat(descriptor.annotations).isEmpty()
      assertThat(descriptor.nullable).isNotNull()
      assertThat(descriptor.serialName).isEqualTo("it.neckar.rest.openapi.OpenApiJsonGeneratorSealedTest.FooWithPropOfSealedSubclass")
      assertThat(descriptor.capturedKClass).isNull() //is always null for non context serializers

      assertThat(descriptor.elementsCount).isEqualTo(1)
      assertThat(descriptor.getElementName(0)).isEqualTo("companyProfile")
      assertThat(descriptor.getElementIndex("companyProfile")).isEqualTo(0)
      assertThat(descriptor.getElementAnnotations(0)).isEmpty()

      val companyProfileDescriptor = descriptor.getElementDescriptor(0)
      assertThat(companyProfileDescriptor).isNotNull()
      assertThat(companyProfileDescriptor.serialName).isEqualTo("Main")
      assertThat(companyProfileDescriptor.annotations).isEmpty()

      assertThat(companyProfileDescriptor.elementsCount).isEqualTo(1)
      assertThat(companyProfileDescriptor.capturedKClass).isNull()
      assertThat(companyProfileDescriptor.getElementName(0)).isEqualTo("companyCode")
      assertThat(companyProfileDescriptor.kind).isEqualTo(StructureKind.CLASS)

      assertThat(companyProfileDescriptor.getElementDescriptor(0).kind).isEqualTo(PrimitiveKind.STRING)
    }

    assertThat(serializer.javaClass.name).isEqualTo($$$"it.neckar.rest.openapi.OpenApiJsonGeneratorSealedTest$FooWithPropOfSealedSubclass$$serializer".trimIndent())
  }

  @Test
  fun testSealedSubclassAsDirectType() {
    val generated: JsonObject

    val expectedCalls = mutableSetOf(
      SchemaDescriptor(typeOf<MyMainCompanyProfile>()),
    )

    with(generator) {
      generated = buildJsonObject {
        buildSchemaType(SchemaDescriptor.referenceObject<FooWithPropOfSealedSubclass>()) {
          assertThat(expectedCalls.remove(it), "Did not expected [$it]").isTrue()
        }
      }
      generated
    }

    assertThat(expectedCalls).isEmpty()
    assertThat(generated).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiJsonGeneratorSealedTest.FooWithPropOfSealedSubclass" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorSealedTest.FooWithPropOfSealedSubclass",
            "properties" : {
              "companyProfile" : {
                "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyMainCompanyProfile"
              }
            },
            "required" : [ "companyProfile" ]
          }
        }
      """.trimIndent()
    )
  }

  /**
   * References one subclass of a sealed interface
   */
  @Serializable
  private data class FooWithPropOfSealedSubclass(
    val companyProfile: MyMainCompanyProfile,
  )

  @Test
  fun testGenerateTyp1e() {
    val generatedSealedInterface: JsonObject
    val expectedCalls = mutableSetOf(
      SchemaDescriptor(typeOf<MyMainCompanyProfile>()),
      SchemaDescriptor(typeOf<MyPartnerCompanyProfile>()),
      SchemaDescriptor(typeOf<MyTestCompanyProfile>()),
    )

    with(generator) {
      generatedSealedInterface = buildJsonObject {
        buildSchemaType(SchemaDescriptor.referenceObject<MyCompanyProfile>()) {
          assertThat(expectedCalls.remove(it), "Did not expected [$it]").isTrue()
        }
      }
      generatedSealedInterface
    }

    assertThat(expectedCalls).isEmpty()
    assertThat(generatedSealedInterface).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiJsonGeneratorSealedTest.MyCompanyProfile" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorSealedTest.MyCompanyProfile",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyMainCompanyProfile.WithType"
            }, {
              "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyPartnerCompanyProfile.WithType"
            }, {
              "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyTestCompanyProfile.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Main" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyMainCompanyProfile.WithType",
                "Partner" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyPartnerCompanyProfile.WithType",
                "Test" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyTestCompanyProfile.WithType"
              }
            }
          }
        }
      """.trimIndent()
    )

    //Now create the subtypes
    val generatedSealedClass: JsonObject
    with(generator) {
      generatedSealedClass = buildJsonObject {
        buildSchemaType(SchemaDescriptor.referenceObject<MyMainCompanyProfile>()) {
          fail("must not be called with type [$it]")
        }
      }
      generatedSealedInterface
    }

    assertThat(generatedSealedClass).isOpenApiEqualTo(
      //language=JSON
      """
        {
          "OpenApiJsonGeneratorSealedTest.MyMainCompanyProfile" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorSealedTest.MyMainCompanyProfile",
            "properties" : {
              "companyCode" : {
                "type" : "string"
              }
            },
            "required" : [ "companyCode" ]
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testNormalInListTest() {
    val serializer = FooBarList.serializer()
    assertThat(serializer.descriptor.elementsCount).isEqualTo(1)

    val detected = mutableListOf<SchemaDescriptor>()

    val generated = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generatePropertyStatementForSchema(
      context = NamedPropertyCreationContext(FooBarList::class, serializer.descriptor, typeOf<FooBarList>(), "fooBars"),
      propertyType = typeOf<List<FooBar>>(),
      propertyDescriptor = serializer.descriptor.getElementDescriptor(0),
      forceType = null,
      schemaConstraints = SchemaPropertyConstraints.Required,
      registerCompositeType = {
        detected.add(it)
      },
    )

    assertThat(generated).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "array",
          "items" : {
            "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.FooBar"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testSealedInListTest() {
    val serializer = MyCompanyProfileList.serializer()
    assertThat(serializer.descriptor.elementsCount).isEqualTo(1)

    assertThat(
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generatePropertyStatementForSchema(
        context = NamedPropertyCreationContext(MyCompanyProfileList::class, serializer.descriptor, typeOf<MyCompanyProfileList>(), "fooBars"),
        propertyType = typeOf<List<MyCompanyProfile>>(),
        propertyDescriptor = serializer.descriptor.getElementDescriptor(0),
        forceType = null,
        schemaConstraints = SchemaPropertyConstraints.Required,
        {
        },
      )
    ).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "array",
          "items" : {
            "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyCompanyProfile"
          }
        }
      """.trimIndent()
    )

    assertThat(
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generatePropertyStatementForSchema(
        context = NamedPropertyCreationContext(MyCompanyProfileList::class, serializer.descriptor, typeOf<MyCompanyProfileList>(), "fooBars"),
        propertyType = typeOf<List<MyCompanyProfile>>(),
        propertyDescriptor = serializer.descriptor.getElementDescriptor(0),
        forceType = null,
        schemaConstraints = SchemaPropertyConstraints.Optional,
        {
        },
      )
    ).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "array",
          "items" : {
            "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyCompanyProfile"
          }
        }
      """.trimIndent()
    )

    assertThat(
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generatePropertyStatementForSchema(
        context = NamedPropertyCreationContext(MyCompanyProfileList::class, serializer.descriptor, typeOf<MyCompanyProfileList>(), "fooBars"),
        propertyType = typeOf<List<MyCompanyProfile>>(),
        propertyDescriptor = serializer.descriptor.getElementDescriptor(0),
        forceType = null,
        schemaConstraints = SchemaPropertyConstraints.OptionalNullable,
        {
        },
      )
    ).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "array",
          "items" : {
            "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyCompanyProfile"
          }
        }
      """.trimIndent()
    )

    assertThat(
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generatePropertyStatementForSchema(
        context = NamedPropertyCreationContext(MyCompanyProfileList::class, serializer.descriptor, typeOf<MyCompanyProfileList>(), "fooBars"),
        propertyType = typeOf<List<MyCompanyProfile>>(),
        propertyDescriptor = serializer.descriptor.getElementDescriptor(0),
        forceType = null,
        schemaConstraints = SchemaPropertyConstraints.RequiredNullable,
        {
        },
      )
    ).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "array",
          "items" : {
            "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyCompanyProfile"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testSealedClass() {
    val fooBarSerializer = FooBar.serializer()
    assertThat(fooBarSerializer.descriptor.elementsCount).isEqualTo(1)

    val detected = mutableListOf<SchemaDescriptor>()

    assertThat(buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildSealedClassPropertyElement(
        elementDescriptor = fooBarSerializer.descriptor.getElementDescriptor(0),
        forcedType = null,
        propertyType = typeOf<MyCompanyProfile>(),
        schemaConstraints = SchemaPropertyConstraints.Required,
      ) {
        detected.add(it)
      }
    }.toString()).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyCompanyProfile"
        }
      """.trimIndent()
    )

    assertThat(detected).hasSize(1)

    assertThat(buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildSealedClassPropertyElement(
        elementDescriptor = fooBarSerializer.descriptor.getElementDescriptor(0),
        forcedType = null,
        propertyType = typeOf<MyCompanyProfile>(),
        schemaConstraints = SchemaPropertyConstraints.RequiredNullable,
      ) {
        detected.add(it)
      }
    }.toString()).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "oneOf" : [ {
            "$ref" : "#/components/schemas/OpenApiJsonGeneratorSealedTest.MyCompanyProfile"
          }, {
            "type" : "null"
          } ]
        }
      """.trimIndent()
    )
  }

  @Test
  fun testMore() {
    //Sealed interface
    assertThat(MyCompanyProfile.serializer().descriptor.kind).isEqualTo(PolymorphicKind.SEALED)

    //Class implementing sealed interface
    MyMainCompanyProfile.serializer().descriptor.let { descriptor ->
      assertThat(descriptor.kind).isEqualTo(StructureKind.CLASS)
      assertThat(descriptor.serialName).isEqualTo("Main") //as defined in the annotation!
    }
  }

  @Serializable
  private data class FooBarList(
    val fooBars: List<FooBar>,
  )

  @Serializable
  private data class MyCompanyProfileList(
    val fooBars: List<MyCompanyProfile>,
  )

  @Serializable
  private data class FooBar(
    val companyProfile: MyCompanyProfile,
  )

  @Serializable
  private sealed interface MyCompanyProfile {
    val companyCode: String
  }

  @Serializable
  @SerialName("Main")
  private data class MyMainCompanyProfile(
    override val companyCode: String,
  ) : MyCompanyProfile

  @Serializable
  @SerialName("Partner")
  private data class MyPartnerCompanyProfile(
    override val companyCode: String,
    val parentCompany: String,
  ) : MyCompanyProfile


  @Serializable
  @SerialName("Test")
  private data class MyTestCompanyProfile(
    override val companyCode: String,
  ) : MyCompanyProfile
}
