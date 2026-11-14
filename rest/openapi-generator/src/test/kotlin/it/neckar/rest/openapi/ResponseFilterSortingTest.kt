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
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.rest.RestResponse
import it.neckar.rest.RestResponseFilteredSorted
import it.neckar.rest.RestResponseFilters
import it.neckar.rest.RestResponseSorts
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.filtering.enumFilterType
import it.neckar.rest.pagination.Page
import it.neckar.rest.pagination.PaginationInfo
import it.neckar.rest.sorting.Sort
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.reflect.full.starProjectedType
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OpenApiRouteBuilder
import it.neckar.rest.openapi.dsl.build

/**
 *
 */
class ResponseFilterSortingTest {

  @Test
  fun testRealisticResponse() {
    val openApiRouteBuilder = OpenApiRouteBuilder(
      path = "/daPath", method = HttpMethod.Get, pathParameterNames = emptyList(), sourceInformation = null,
    ).apply {

      responding<FooBarResponse>(
        successExample = FooBarResponse.good(Page.single(listOf(FooBar("foo", 42, false)))),
        failureExample = FooBarResponse.bad(),
      )
    }

    val route = openApiRouteBuilder.build()

    OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).collectTypesToGenerate(listOf(route)).let { typesToGenerate ->
      assertThat(typesToGenerate).hasSize(4)
    }


    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)

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
            "/daPath" : {
              "get" : {
                "operationId" : "GetDaPath",
                "x-category" : "default",
                "parameters" : [ {
                  "name" : "foo",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.FilterCriteria",
                  "schema" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                }, {
                  "name" : "bar",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.FilterCriteria",
                  "schema" : {
                    "type" : "boolean",
                    "x-source-kotlin-type" : "kotlin.Boolean"
                  }
                }, {
                  "name" : "bazOption",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.FilterCriteria",
                  "schema" : {
                    "type" : "integer",
                    "x-source-kotlin-type" : "kotlin.Int"
                  }
                }, {
                  "name" : "boDateTime",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.FilterCriteria",
                  "schema" : {
                    "type" : "string",
                    "format" : "date-time",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                }, {
                  "name" : "biDate",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.FilterCriteria",
                  "schema" : {
                    "type" : "string",
                    "format" : "date",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                }, {
                  "name" : "someUuid",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.FilterCriteria",
                  "schema" : {
                    "type" : "string",
                    "format" : "uuid",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                }, {
                  "name" : "status",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.FilterCriteria",
                  "schema" : {
                    "$ref" : "#/components/schemas/ResponseFilterSortingTest.TestStatus"
                  }
                }, {
                  "name" : "scope",
                  "in" : "query",
                  "required" : false,
                  "description" : "Comma-separated list of filter keys that define the query scope. Scope filters affect totalElementsCount in pagination.",
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.FilterCriteria",
                  "schema" : {
                    "type" : "string",
                    "enum" : [ "foo", "bar", "bazOption", "boDateTime", "biDate", "someUuid", "status" ],
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                }, {
                  "name" : "sortBy",
                  "in" : "query",
                  "required" : false,
                  "schema" : {
                    "$ref" : "#/components/schemas/ResponseFilterSortingTest.FooBarResponse.SortCriteria"
                  }
                }, {
                  "name" : "sortOrder",
                  "in" : "query",
                  "required" : false,
                  "schema" : {
                    "$ref" : "#/components/schemas/Sort.Order"
                  }
                }, {
                  "name" : "pageIndex",
                  "in" : "query",
                  "required" : false,
                  "schema" : {
                    "type" : "integer",
                    "x-source-kotlin-type" : "kotlin.Int"
                  }
                }, {
                  "name" : "pageSize",
                  "in" : "query",
                  "required" : false,
                  "schema" : {
                    "type" : "integer",
                    "x-source-kotlin-type" : "kotlin.Int"
                  }
                } ],
                "responses" : {
                  "200" : {
                    "description" : "Responds with Good or Bad types",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/ResponseFilterSortingTest.FooBarResponse"
                        },
                        "examples" : {
                          "good" : {
                            "summary" : "Good",
                            "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.Good",
                            "x-type-ref" : "#/components/schemas/ResponseFilterSortingTest.FooBarResponse.Good.WithType",
                            "value" : {
                              "data" : [ {
                                "foo" : "foo",
                                "bar" : 42,
                                "bazOption" : false
                              } ],
                              "paginationInfo" : {
                                "pageIndex" : 0,
                                "pageSize" : -1,
                                "pageCount" : 1,
                                "filteredElementsCount" : 1,
                                "totalElementsCount" : 1
                              },
                              "type" : "good"
                            }
                          },
                          "bad" : {
                            "summary" : "Bad",
                            "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.Failed",
                            "x-type-ref" : "#/components/schemas/ResponseFilterSortingTest.FooBarResponse.Failed",
                            "value" : {
                              "type" : "bad/Failed"
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
              "ResponseFilterSortingTest.FooBarResponse.SortCriteria" : {
                "type" : "string",
                "enum" : [ "Foo", "Bar", "BazOption" ]
              },
              "Sort.Order" : {
                "type" : "string",
                "enum" : [ "Asc", "Desc" ]
              },
              "ResponseFilterSortingTest.TestStatus" : {
                "type" : "string",
                "enum" : [ "Active", "Inactive", "Pending" ]
              },
              "ResponseFilterSortingTest.FooBarResponse" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse",
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/ResponseFilterSortingTest.FooBarResponse.Failed"
                }, {
                  "$ref" : "#/components/schemas/ResponseFilterSortingTest.FooBarResponse.Good.WithType"
                } ],
                "discriminator" : {
                  "propertyName" : "type",
                  "mapping" : {
                    "bad/Failed" : "#/components/schemas/ResponseFilterSortingTest.FooBarResponse.Failed",
                    "good" : "#/components/schemas/ResponseFilterSortingTest.FooBarResponse.Good.WithType"
                  }
                }
              },
              "ResponseFilterSortingTest.FooBarResponse.Good.WithType" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.Good",
                "allOf" : [ {
                  "$ref" : "#/components/schemas/ResponseFilterSortingTest.FooBarResponse.Good"
                } ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "good" ]
                  }
                },
                "required" : [ "type" ]
              },
              "ResponseFilterSortingTest.FooBarResponse.Good" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.Good",
                "properties" : {
                  "data" : {
                    "type" : "array",
                    "items" : {
                      "$ref" : "#/components/schemas/ResponseFilterSortingTest.FooBar"
                    }
                  },
                  "paginationInfo" : {
                    "$ref" : "#/components/schemas/PaginationInfo"
                  }
                },
                "required" : [ "data", "paginationInfo" ]
              },
              "PaginationInfo" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.pagination.PaginationInfo",
                "properties" : {
                  "pageIndex" : {
                    "x-source-kotlin-type" : "it.neckar.rest.pagination.PageIndex",
                    "type" : "integer"
                  },
                  "pageSize" : {
                    "x-source-kotlin-type" : "it.neckar.rest.pagination.PageSize",
                    "type" : "integer"
                  },
                  "pageCount" : {
                    "type" : "integer"
                  },
                  "filteredElementsCount" : {
                    "type" : "integer"
                  },
                  "totalElementsCount" : {
                    "type" : "integer"
                  }
                },
                "required" : [ "pageIndex", "pageSize", "pageCount", "filteredElementsCount", "totalElementsCount" ]
              },
              "ResponseFilterSortingTest.FooBar" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBar",
                "properties" : {
                  "foo" : {
                    "type" : "string"
                  },
                  "bar" : {
                    "type" : "integer"
                  },
                  "bazOption" : {
                    "type" : [ "boolean", "null" ]
                  }
                },
                "required" : [ "foo", "bar" ]
              },
              "ResponseFilterSortingTest.FooBarResponse.Failed" : {
                "type" : "object",
                "x-source-kotlin-type" : "it.neckar.rest.openapi.ResponseFilterSortingTest.FooBarResponse.Failed",
                "required" : [ "type" ],
                "properties" : {
                  "type" : {
                    "type" : "string",
                    "enum" : [ "bad/Failed" ]
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

  /**
   * Response for
   */
  @Serializable
  private sealed interface FooBarResponse : RestResponseFilteredSorted<List<FooBar>, FooBarResponse.FilterCriteria, FooBarResponse.SortCriteria> {

    @Serializable
    @SerialName("good")
    data class Good(
      override val data: List<FooBar>,
      override val paginationInfo: PaginationInfo,
    ) : FooBarResponse, RestResponse.PaginatedGoodResponse<List<FooBar>>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : FooBarResponse, RestResponse.BadResponse<List<FooBar>>

    companion object {
      /**
       * Helper method to create a success object - with the correct type
       */
      fun good(page: Page<FooBar>): FooBarResponse = Good(page.pageElements, page.paginationInfo)

      /**
       * Helper method to create a failure object - with the correct type
       */
      fun bad(): FooBarResponse = Failed
    }

    enum class SortCriteria : RestResponseSorts {
      Foo,
      Bar,
      BazOption,
      ;
    }

    enum class FilterCriteria(
      override val filterType: TypedFilterKey.FilterType = TypedFilterKey.FilterType.String,
    ) : RestResponseFilters {
      Foo,
      Bar(TypedFilterKey.FilterType.Boolean),
      BazOption(TypedFilterKey.FilterType.Int),
      BoDateTime(TypedFilterKey.FilterType.Instant),
      BiDate(TypedFilterKey.FilterType.Date),
      SomeUuid(TypedFilterKey.FilterType.Uuid),
      Status(enumFilterType<TestStatus>()),
    }
  }


  @Serializable
  private data class FooBar(
    val foo: String,
    val bar: Int,
    val bazOption: Boolean? = null,
  )

  /**
   * Test enum for enum filter testing
   */
  internal enum class TestStatus {
    Active,
    Inactive,
    Pending,
  }
}



