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
import io.ktor.http.*
import it.neckar.open.http.UrlParameterName
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.rest.RestResponse
import it.neckar.rest.patch.Patchable
import it.neckar.rest.patch.PatchableRequired
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OpenApiRouteBuilder
import it.neckar.rest.openapi.dsl.build

/**
 * Real-life test for OpenAPI schema generation with PATCH-style PUT request.
 *
 * This test demonstrates a complete API endpoint for patching a User entity.
 */
class OpenApiPatchRouteTest {

  /**
   * The User entity that we want to patch.
   */
  @Serializable
  private data class User(
    val id: Uuid,
    val username: String,
    val email: String,
    val displayName: String?,
    val bio: String?,
    val age: Int,
  )

  /**
   * PATCH request for updating a User.
   *
   * Uses [Patchable] for nullable fields (where null means "delete")
   * and [PatchableRequired] for non-nullable fields (where null is not allowed).
   */
  @Serializable
  private data class UserPatchRequest(
    /**
     * ID is always required to identify the resource to patch.
     */
    val id: Uuid,

    /**
     * Username cannot be null, only updated or kept.
     */
    val username: PatchableRequired<String> = PatchableRequired.Keep,

    /**
     * Email cannot be null, only updated or kept.
     */
    val email: PatchableRequired<String> = PatchableRequired.Keep,

    /**
     * Age cannot be null, only updated or kept.
     */
    val age: PatchableRequired<Int> = PatchableRequired.Keep,

    /**
     * Display name can be set to null (deleted), updated, or kept.
     */
    val displayName: Patchable<String> = Patchable.Keep,

    /**
     * Bio can be set to null (deleted), updated, or kept.
     */
    val bio: Patchable<String> = Patchable.Keep,
  )

  /**
   * Response for the PATCH operation.
   */
  @Serializable
  private sealed interface UserPatchResponse : RestResponse<User> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: User) : UserPatchResponse, RestResponse.GoodResponse<User>

    @Serializable
    @SerialName("bad/NotFound")
    data object NotFound : UserPatchResponse, RestResponse.BadResponse<User>

    @Serializable
    @SerialName("bad/ValidationError")
    data class ValidationError(val message: String) : UserPatchResponse, RestResponse.BadResponse<User>

    companion object {
      fun good(user: User): UserPatchResponse = Good(user)
      fun notFound(): UserPatchResponse = NotFound
      fun validationError(message: String): UserPatchResponse = ValidationError(message)
    }
  }

  @Test
  fun `complete PATCH endpoint schema generation`() {
    val sampleUserId = Uuid.parse("550e8400-e29b-41d4-a716-446655440000")
    val sampleUser = User(
      id = sampleUserId,
      username = "johndoe",
      email = "john@example.com",
      displayName = "John Doe",
      bio = "Software developer",
      age = 30,
    )

    val route = OpenApiRouteBuilder(
      path = "/api/users/{userId}",
      method = HttpMethod.Put,
      pathParameterNames = listOf(UrlParameterName("userId")),
      sourceInformation = null,
    ).apply {
      summary = "Update User"
      description = "Partially updates a user. Only provided fields will be updated. Use null to delete optional fields."

      requestBody<UserPatchRequest> {
        description = "The patch request containing fields to update"
        example(
          "update-username",
          UserPatchRequest(
            id = sampleUserId,
            username = PatchableRequired.Update("newusername"),
          ),
          summary = "Update only username",
        )
        example(
          "delete-bio",
          UserPatchRequest(
            id = sampleUserId,
            bio = Patchable.Delete,
          ),
          summary = "Delete the bio field",
        )
        example(
          "update-multiple",
          UserPatchRequest(
            id = sampleUserId,
            email = PatchableRequired.Update("newemail@example.com"),
            displayName = Patchable.Update("New Display Name"),
            age = PatchableRequired.Update(31),
          ),
          summary = "Update multiple fields",
        )
      }

      response("User updated successfully") {
        body<UserPatchResponse> {
          description = "The response containing the updated user or an error"
          example("good", UserPatchResponse.good(sampleUser), "Successful update")
          example("bad/NotFound", UserPatchResponse.notFound(), "User not found")
          example("bad/ValidationError", UserPatchResponse.validationError("Invalid email format"), "Validation failed")
        }
      }
    }.build()

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(
      routes = listOf(route),
      metaData = OpenApiMetaData.default,
      additionalTypes = emptyList(),
    )

    assertThat(json).isOpenApiEqualTo(
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
          "paths" : {
            "/api/users/{userId}" : {
              "put" : {
                "description" : "Partially updates a user. Only provided fields will be updated. Use null to delete optional fields.",
                "summary" : "Update User",
                "operationId" : "PutUser",
                "x-category" : "default",
                "parameters" : [ {
                  "name" : "userId",
                  "in" : "path",
                  "required" : true,
                  "schema" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                } ],
                "requestBody" : {
                  "required" : true,
                  "content" : {
                    "application/json" : {
                      "schema" : {
                        "$ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchRequest"
                      },
                      "examples" : {
                        "update-username" : {
                          "summary" : "Update only username",
                          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchRequest",
                          "x-type-ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchRequest",
                          "value" : {
                            "id" : "550e8400-e29b-41d4-a716-446655440000",
                            "username" : {
                              "type" : "Update",
                              "value" : "newusername"
                            },
                            "email" : {
                              "type" : "Keep"
                            },
                            "age" : {
                              "type" : "Keep"
                            },
                            "displayName" : {
                              "type" : "Keep"
                            },
                            "bio" : {
                              "type" : "Keep"
                            }
                          }
                        },
                        "delete-bio" : {
                          "summary" : "Delete the bio field",
                          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchRequest",
                          "x-type-ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchRequest",
                          "value" : {
                            "id" : "550e8400-e29b-41d4-a716-446655440000",
                            "username" : {
                              "type" : "Keep"
                            },
                            "email" : {
                              "type" : "Keep"
                            },
                            "age" : {
                              "type" : "Keep"
                            },
                            "displayName" : {
                              "type" : "Keep"
                            },
                            "bio" : {
                              "type" : "Delete"
                            }
                          }
                        },
                        "update-multiple" : {
                          "summary" : "Update multiple fields",
                          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchRequest",
                          "x-type-ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchRequest",
                          "value" : {
                            "id" : "550e8400-e29b-41d4-a716-446655440000",
                            "username" : {
                              "type" : "Keep"
                            },
                            "email" : {
                              "type" : "Update",
                              "value" : "newemail@example.com"
                            },
                            "age" : {
                              "type" : "Update",
                              "value" : 31
                            },
                            "displayName" : {
                              "type" : "Update",
                              "value" : "New Display Name"
                            },
                            "bio" : {
                              "type" : "Keep"
                            }
                          }
                        }
                      }
                    }
                  }
                },
                "responses" : {
                  "200" : {
                    "description" : "User updated successfully",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse"
                        },
                        "examples" : {
                          "good" : {
                            "summary" : "Successful update",
                            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchResponse.Good",
                            "x-type-ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.Good.WithType",
                            "value" : {
                              "data" : {
                                "id" : "550e8400-e29b-41d4-a716-446655440000",
                                "username" : "johndoe",
                                "email" : "john@example.com",
                                "displayName" : "John Doe",
                                "bio" : "Software developer",
                                "age" : 30
                              },
                              "type" : "good"
                            }
                          },
                          "bad/NotFound" : {
                            "summary" : "User not found",
                            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchResponse.NotFound",
                            "x-type-ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.NotFound",
                            "value" : {
                              "type" : "bad/NotFound"
                            }
                          },
                          "bad/ValidationError" : {
                            "summary" : "Validation failed",
                            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchResponse.ValidationError",
                            "x-type-ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.ValidationError.WithType",
                            "value" : {
                              "message" : "Invalid email format",
                              "type" : "bad/ValidationError"
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          },
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
              "OpenApiPatchRouteTest.UserPatchRequest" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchRequest",
                "properties" : {
                  "id" : {
                    "type" : "string",
                    "format" : "uuid"
                  },
                  "username" : {
                    "$ref" : "#/components/schemas/PatchableRequired__String__"
                  },
                  "email" : {
                    "$ref" : "#/components/schemas/PatchableRequired__String__"
                  },
                  "age" : {
                    "$ref" : "#/components/schemas/PatchableRequired__Int__"
                  },
                  "displayName" : {
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
              },
              "OpenApiPatchRouteTest.UserPatchResponse" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchResponse",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.Good.WithType"
                }, {
                  "$ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.NotFound"
                }, {
                  "$ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.ValidationError.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "good" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.Good.WithType",
                    "bad/NotFound" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.NotFound",
                    "bad/ValidationError" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.ValidationError.WithType"
                  }
                }
              },
              "OpenApiPatchRouteTest.UserPatchResponse.ValidationError.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchResponse.ValidationError",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.ValidationError"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "bad/ValidationError" ]
                  }
                },
                "required" : [ "type" ]
              },
              "OpenApiPatchRouteTest.UserPatchResponse.ValidationError" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchResponse.ValidationError",
                "properties" : {
                  "message" : {
                    "type" : "string"
                  }
                },
                "required" : [ "message" ]
              },
              "OpenApiPatchRouteTest.UserPatchResponse.Good.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchResponse.Good",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiPatchRouteTest.UserPatchResponse.Good"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "good" ]
                  }
                },
                "required" : [ "type" ]
              },
              "OpenApiPatchRouteTest.UserPatchResponse.Good" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchResponse.Good",
                "properties" : {
                  "data" : {
                    "$ref" : "#/components/schemas/OpenApiPatchRouteTest.User"
                  }
                },
                "required" : [ "data" ]
              },
              "OpenApiPatchRouteTest.User" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.User",
                "properties" : {
                  "id" : {
                    "type" : "string",
                    "format" : "uuid"
                  },
                  "username" : {
                    "type" : "string"
                  },
                  "email" : {
                    "type" : "string"
                  },
                  "displayName" : {
                    "type" : [ "string", "null" ]
                  },
                  "bio" : {
                    "type" : [ "string", "null" ]
                  },
                  "age" : {
                    "type" : "integer"
                  }
                },
                "required" : [ "id", "username", "email", "displayName", "bio", "age" ]
              },
              "OpenApiPatchRouteTest.UserPatchResponse.NotFound" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiPatchRouteTest.UserPatchResponse.NotFound",
                "required" : [ "type" ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "bad/NotFound" ]
                  }
                },
                "x-ignored-properties" : [ "isBad", "isGood" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }
}
