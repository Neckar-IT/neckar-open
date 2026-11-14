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
import it.neckar.rest.patch.Patchable
import it.neckar.rest.patch.PatchableRequired
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import kotlin.uuid.Uuid
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 * Tests for OpenAPI schema generation of [Patchable] and [PatchableRequired].
 */
class OpenApiPatchableTest {
  @Serializable
  private data class Request1(
    //Either a value (UPDATE) or undefined (KEEP) - null is *not* allowed here
    val username: PatchableRequired<String> = PatchableRequired.Keep,
  )

  @Test
  fun testSuperSimplePatch() {
    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(
      listOf(SchemaDescriptor.of<Request1>())
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiPatchableTest.Request1" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchableTest.Request1",
            "properties" : {
              "username" : {
                "$ref" : "#/components/schemas/PatchableRequired__String__"
              }
            }
          },
          "PatchableRequired__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/PatchableRequired.Keep"
            }, {
              "$ref" : "#/components/schemas/PatchableRequired.Update__String__.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Keep" : "#/components/schemas/PatchableRequired.Keep",
                "Update" : "#/components/schemas/PatchableRequired.Update__String__.WithType"
              }
            }
          },
          "PatchableRequired.Update__String__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired.Update",
            "allOf" : [ {
              "$ref" : "#/components/schemas/PatchableRequired.Update__String__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Update" ]
              }
            },
            "required" : [ "type" ]
          },
          "PatchableRequired.Update__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired.Update",
            "properties" : {
              "value" : {
                "type" : "string"
              }
            },
            "required" : [ "value" ]
          },
          "PatchableRequired.Keep" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired.Keep",
            "required" : [ "type" ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Keep" ]
              }
            },
            "x-ignored-properties" : [ "isKeep", "isUpdate" ]
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class Request2(
    //Either a value (UPDATE), null (DELETE) or undefined (KEEP)
    val username: Patchable<String> = Patchable.Keep,
  )

  @Test
  fun testSuperSimplePatchRequest2() {
    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(
      listOf(SchemaDescriptor.of<Request2>())
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiPatchableTest.Request2" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchableTest.Request2",
            "properties" : {
              "username" : {
                "$ref" : "#/components/schemas/Patchable__String__"
              }
            }
          },
          "Patchable__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/Patchable.Delete"
            }, {
              "$ref" : "#/components/schemas/Patchable.Keep"
            }, {
              "$ref" : "#/components/schemas/Patchable.Update__String__.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Delete" : "#/components/schemas/Patchable.Delete",
                "Keep" : "#/components/schemas/Patchable.Keep",
                "Update" : "#/components/schemas/Patchable.Update__String__.WithType"
              }
            }
          },
          "Patchable.Update__String__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable.Update",
            "allOf" : [ {
              "$ref" : "#/components/schemas/Patchable.Update__String__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Update" ]
              }
            },
            "required" : [ "type" ]
          },
          "Patchable.Update__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable.Update",
            "properties" : {
              "value" : {
                "type" : "string"
              }
            },
            "required" : [ "value" ]
          },
          "Patchable.Keep" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable.Keep",
            "required" : [ "type" ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Keep" ]
              }
            },
            "x-ignored-properties" : [ "isDelete", "isKeep", "isUpdate" ]
          },
          "Patchable.Delete" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable.Delete",
            "required" : [ "type" ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Delete" ]
              }
            },
            "x-ignored-properties" : [ "isDelete", "isKeep", "isUpdate" ]
          }
        }
      """.trimIndent()
    )
  }

  /**
   * Example PATCH request with both [Patchable] (nullable) and [PatchableRequired] (non-nullable) fields.
   */
  @Serializable
  private data class UserPatchRequest(
    /**
     * ID is always required to identify the resource to patch
     */
    val id: Uuid,
    val fooNullable: String? = null,
    val fooNullableNoDefault: String? = null,

    // Non-nullable fields - use PatchableRequired (no Delete allowed)
    val username: PatchableRequired<String> = PatchableRequired.Keep,
    val age: PatchableRequired<Int> = PatchableRequired.Keep,
    // Nullable fields - use Patchable (Delete allowed, serializes as null)
    val email: Patchable<String> = Patchable.Keep,
    val bio: Patchable<String> = Patchable.Keep,
  )

  @Test
  fun testSchemaDescriptor() {
    val descriptor = SchemaDescriptor.of<UserPatchRequest>()
    assertThat(descriptor).isInstanceOf<SchemaDescriptor.ClassSchemaObject>()
    descriptor as SchemaDescriptor.ClassSchemaObject

    assertThat(descriptor.getSimpleTypeName()).isEqualTo("OpenApiPatchableTest.UserPatchRequest")
    assertThat(descriptor.getTypeName()).isEqualTo("it.neckar.rest.openapi.OpenApiPatchableTest.UserPatchRequest")

    assertThat(descriptor.reference.unescapedValue).isEqualTo("OpenApiPatchableTest.UserPatchRequest")
    assertThat(descriptor.reference.completeReference()).isEqualTo("#/components/schemas/OpenApiPatchableTest.UserPatchRequest")
    assertThat(descriptor.reference.valueEscaped()).isEqualTo("OpenApiPatchableTest.UserPatchRequest")
  }

  @Test
  fun testSchema() {
    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(
      listOf(SchemaDescriptor.of<UserPatchRequest>())
    )

    assertThat(json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiPatchableTest.UserPatchRequest" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchableTest.UserPatchRequest",
            "properties" : {
              "id" : {
                "type" : "string",
                "format" : "uuid"
              },
              "fooNullable" : {
                "type" : [ "string", "null" ]
              },
              "fooNullableNoDefault" : {
                "type" : [ "string", "null" ]
              },
              "username" : {
                "$ref" : "#/components/schemas/PatchableRequired__String__"
              },
              "age" : {
                "$ref" : "#/components/schemas/PatchableRequired__Int__"
              },
              "email" : {
                "$ref" : "#/components/schemas/Patchable__String__"
              },
              "bio" : {
                "$ref" : "#/components/schemas/Patchable__String__"
              }
            },
            "required" : [ "id" ]
          },
          "Patchable__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/Patchable.Delete"
            }, {
              "$ref" : "#/components/schemas/Patchable.Keep"
            }, {
              "$ref" : "#/components/schemas/Patchable.Update__String__.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Delete" : "#/components/schemas/Patchable.Delete",
                "Keep" : "#/components/schemas/Patchable.Keep",
                "Update" : "#/components/schemas/Patchable.Update__String__.WithType"
              }
            }
          },
          "Patchable.Update__String__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable.Update",
            "allOf" : [ {
              "$ref" : "#/components/schemas/Patchable.Update__String__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Update" ]
              }
            },
            "required" : [ "type" ]
          },
          "Patchable.Update__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable.Update",
            "properties" : {
              "value" : {
                "type" : "string"
              }
            },
            "required" : [ "value" ]
          },
          "Patchable.Keep" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable.Keep",
            "required" : [ "type" ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Keep" ]
              }
            },
            "x-ignored-properties" : [ "isDelete", "isKeep", "isUpdate" ]
          },
          "Patchable.Delete" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.Patchable.Delete",
            "required" : [ "type" ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Delete" ]
              }
            },
            "x-ignored-properties" : [ "isDelete", "isKeep", "isUpdate" ]
          },
          "PatchableRequired__Int__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/PatchableRequired.Keep"
            }, {
              "$ref" : "#/components/schemas/PatchableRequired.Update__Int__.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Keep" : "#/components/schemas/PatchableRequired.Keep",
                "Update" : "#/components/schemas/PatchableRequired.Update__Int__.WithType"
              }
            }
          },
          "PatchableRequired.Update__Int__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired.Update",
            "allOf" : [ {
              "$ref" : "#/components/schemas/PatchableRequired.Update__Int__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Update" ]
              }
            },
            "required" : [ "type" ]
          },
          "PatchableRequired.Update__Int__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired.Update",
            "properties" : {
              "value" : {
                "type" : "integer"
              }
            },
            "required" : [ "value" ]
          },
          "PatchableRequired.Keep" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired.Keep",
            "required" : [ "type" ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Keep" ]
              }
            },
            "x-ignored-properties" : [ "isKeep", "isUpdate" ]
          },
          "PatchableRequired__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired",
            "oneOf" : [ {
              "$ref" : "#/components/schemas/PatchableRequired.Keep"
            }, {
              "$ref" : "#/components/schemas/PatchableRequired.Update__String__.WithType"
            } ],
            "discriminator" : {
              "propertyName" : "type",
              "mapping" : {
                "Keep" : "#/components/schemas/PatchableRequired.Keep",
                "Update" : "#/components/schemas/PatchableRequired.Update__String__.WithType"
              }
            }
          },
          "PatchableRequired.Update__String__.WithType" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired.Update",
            "allOf" : [ {
              "$ref" : "#/components/schemas/PatchableRequired.Update__String__"
            } ],
            "properties" : {
              "type" : {
                "type" : "string",
                "enum" : [ "Update" ]
              }
            },
            "required" : [ "type" ]
          },
          "PatchableRequired.Update__String__" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.patch.PatchableRequired.Update",
            "properties" : {
              "value" : {
                "type" : "string"
              }
            },
            "required" : [ "value" ]
          }
        }
      """.trimIndent()
    )

  }

  @Test
  fun `Patchable and PatchableRequired fields should have correct schema`() {
    val json = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildSchemaTypeUsingSerialDescriptor(typeOf<UserPatchRequest>()) {
        // Ignore composite types for this test
      }
    }

    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "type" : "object",
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchableTest.UserPatchRequest",
          "properties" : {
            "id" : {
              "type" : "string",
              "format" : "uuid"
            },
            "fooNullable" : {
              "type" : [ "string", "null" ]
            },
            "fooNullableNoDefault" : {
              "type" : [ "string", "null" ]
            },
            "username" : {
              "$ref" : "#/components/schemas/PatchableRequired__String__"
            },
            "age" : {
              "$ref" : "#/components/schemas/PatchableRequired__Int__"
            },
            "email" : {
              "$ref" : "#/components/schemas/Patchable__String__"
            },
            "bio" : {
              "$ref" : "#/components/schemas/Patchable__String__"
            }
          },
          "required" : [ "id" ]
        }
      """.trimIndent()
    )
  }
}
