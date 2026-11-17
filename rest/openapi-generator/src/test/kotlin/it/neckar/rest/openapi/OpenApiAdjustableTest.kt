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
import it.neckar.adjustable.AdjustableValue
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.schema.SchemaDescriptor.ComponentSchema.ReferenceType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiAdjustableTest {
  @Test
  fun testSchemaGenericsEquals() {
    val schemaDescriptor = SchemaDescriptor.of<AdjustableValue<String>>()
    assertThat(schemaDescriptor).isInstanceOf(SchemaDescriptor.ClassSchemaObject::class.java)
    schemaDescriptor as SchemaDescriptor.ClassSchemaObject

    assertThat(schemaDescriptor).isEqualTo(SchemaDescriptor.of<AdjustableValue<String>>())
    assertThat(schemaDescriptor).isNotEqualTo(SchemaDescriptor.of<AdjustableValue<Int>>())

    assertThat(SchemaDescriptor.of<AdjustableValue<Int>>().asComponent().reference(ReferenceType.Discriminated).valueEscaped()).isEqualTo("AdjustableValue__Int__")
  }

  @Test
  fun testInner() {
    assertThat(SchemaDescriptor.of<SealedInterface.OptionA>().asDiscriminatedClassSchema().baseSchema.reference(ReferenceType.Discriminated).valueEscaped()).isEqualTo("OpenApiAdjustableTest.SealedInterface.OptionA")
    assertThat(SchemaDescriptor.of<SealedInterface.OptionA>().asDiscriminatedClassSchema().schemaWithDiscriminator.reference(ReferenceType.Discriminated).unescapedValue).isEqualTo("OpenApiAdjustableTest.SealedInterface.OptionA.WithType")
    assertThat(SchemaDescriptor.of<SealedInterface.OptionA>().asDiscriminatedClassSchema().baseSchema.reference(ReferenceType.Discriminated).completeReference()).isEqualTo("#/components/schemas/OpenApiAdjustableTest.SealedInterface.OptionA")
  }

  @Test
  fun testIt() {
    assertThat(SchemaDescriptor.of<AdjustableWrapperWithString>().toString()).isEqualTo("ClassSchemaObject(referenceName=OpenApiAdjustableTest.AdjustableWrapperWithString)")
    assertThat(SchemaDescriptor.of<AdjustableWrapperWithInt>().toString()).isEqualTo("ClassSchemaObject(referenceName=OpenApiAdjustableTest.AdjustableWrapperWithInt)")

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(
      listOf(
        SchemaDescriptor.of<AdjustableWrapperWithString>(),
        SchemaDescriptor.of<AdjustableWrapperWithInt>(),
      )
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiAdjustableTest.AdjustableWrapperWithString" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiAdjustableTest.AdjustableWrapperWithString",
            "properties" : {
              "value" : {
                "$ref" : "#/components/schemas/AdjustableValue__String__"
              }
            },
            "required" : [ "value" ]
          },
          "AdjustableValue__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/AdjustableValue.Adjusted__String__.WithType"
            }, {
              "$ref" : "#/components/schemas/AdjustableValue.Auto__String__.WithType"
            }, {
              "$ref" : "#/components/schemas/AdjustableValue.Manual__String__.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Adjusted" : "#/components/schemas/AdjustableValue.Adjusted__String__.WithType",
                "Auto" : "#/components/schemas/AdjustableValue.Auto__String__.WithType",
                "Manual" : "#/components/schemas/AdjustableValue.Manual__String__.WithType"
              }
            }
          },
          "AdjustableValue.Manual__String__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Manual",
            "allOf" : [ {
              "$ref" : "#/components/schemas/AdjustableValue.Manual__String__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Manual" ]
              }
            },
            "required" : [ "type" ]
          },
          "AdjustableValue.Manual__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Manual",
            "properties" : {
              "value" : {
                "type" : "string"
              }
            },
            "required" : [ "value" ]
          },
          "AdjustableValue.Auto__String__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Auto",
            "allOf" : [ {
              "$ref" : "#/components/schemas/AdjustableValue.Auto__String__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Auto" ]
              }
            },
            "required" : [ "type" ]
          },
          "AdjustableValue.Auto__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Auto",
            "properties" : {
              "value" : {
                "type" : "string"
              }
            },
            "required" : [ "value" ]
          },
          "AdjustableValue.Adjusted__String__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Adjusted",
            "allOf" : [ {
              "$ref" : "#/components/schemas/AdjustableValue.Adjusted__String__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Adjusted" ]
              }
            },
            "required" : [ "type" ]
          },
          "AdjustableValue.Adjusted__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Adjusted",
            "properties" : {
              "calculated" : {
                "type" : "string"
              },
              "value" : {
                "type" : "string"
              }
            },
            "required" : [ "calculated", "value" ]
          },
          "OpenApiAdjustableTest.AdjustableWrapperWithInt" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiAdjustableTest.AdjustableWrapperWithInt",
            "properties" : {
              "value" : {
                "$ref" : "#/components/schemas/AdjustableValue__Int__"
              }
            },
            "required" : [ "value" ]
          },
          "AdjustableValue__Int__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/AdjustableValue.Adjusted__Int__.WithType"
            }, {
              "$ref" : "#/components/schemas/AdjustableValue.Auto__Int__.WithType"
            }, {
              "$ref" : "#/components/schemas/AdjustableValue.Manual__Int__.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Adjusted" : "#/components/schemas/AdjustableValue.Adjusted__Int__.WithType",
                "Auto" : "#/components/schemas/AdjustableValue.Auto__Int__.WithType",
                "Manual" : "#/components/schemas/AdjustableValue.Manual__Int__.WithType"
              }
            }
          },
          "AdjustableValue.Manual__Int__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Manual",
            "allOf" : [ {
              "$ref" : "#/components/schemas/AdjustableValue.Manual__Int__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Manual" ]
              }
            },
            "required" : [ "type" ]
          },
          "AdjustableValue.Manual__Int__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Manual",
            "properties" : {
              "value" : {
                "type" : "integer"
              }
            },
            "required" : [ "value" ]
          },
          "AdjustableValue.Auto__Int__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Auto",
            "allOf" : [ {
              "$ref" : "#/components/schemas/AdjustableValue.Auto__Int__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Auto" ]
              }
            },
            "required" : [ "type" ]
          },
          "AdjustableValue.Auto__Int__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Auto",
            "properties" : {
              "value" : {
                "type" : "integer"
              }
            },
            "required" : [ "value" ]
          },
          "AdjustableValue.Adjusted__Int__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Adjusted",
            "allOf" : [ {
              "$ref" : "#/components/schemas/AdjustableValue.Adjusted__Int__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Adjusted" ]
              }
            },
            "required" : [ "type" ]
          },
          "AdjustableValue.Adjusted__Int__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.adjustable.AdjustableValue.Adjusted",
            "properties" : {
              "calculated" : {
                "type" : "integer"
              },
              "value" : {
                "type" : "integer"
              }
            },
            "required" : [ "calculated", "value" ]
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class AdjustableWrapperWithString(
    val value: AdjustableValue<String>
  )

  @Serializable
  private data class AdjustableWrapperWithInt(
    val value: AdjustableValue<Int>
  )

  @Serializable
  private sealed interface SealedInterface {
    @Serializable
    @SerialName("OptionA")
    data class OptionA(val a: String) : SealedInterface
  }
}
