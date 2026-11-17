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
import it.neckar.open.http.UrlParameterName
import it.neckar.open.kotlin.lang.simpleNameWithEnclosing
import it.neckar.open.test.utils.isOpenApiEqualTo
import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.HasTypedFilterKey
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.filtering.double
import it.neckar.rest.filtering.enum
import it.neckar.rest.filtering.string
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OpenApiRequestBuilder
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.dsl.filteredBy
import it.neckar.rest.openapi.dsl.openApiRoute
import it.neckar.rest.openapi.dsl.paginated
import it.neckar.rest.openapi.dsl.sortedBy

/**
 * Test for OpenAPI JSON generation with Enum filter parameters
 */
class OpenApiEnumFilterParameterTest {
  enum class StatusEnum {
    Active,
    Inactive,
    Pending
  }

  enum class PriorityEnum {
    Low,
    Medium,
    High
  }

  enum class MyFilterKeys(override val typedFilterKey: TypedFilterKey) : HasTypedFilterKey {
    Status(FilterKey("status").enum(StatusEnum::class)),
    Priority(FilterKey("priority").enum(PriorityEnum::class)),
  }

  @Test
  fun testEnumFilterParameters() {
    val route = openApiRoute("/items", HttpMethod.Get, emptyList()) {
      summary = "Get items with enum filters"

      request {
        filteredBy(MyFilterKeys.entries.toList(), MyFilterKeys::class)
      }

      response("Success") {
        statusCode(HttpStatusCode.OK)
        noExamples()
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
      .generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
      .toString()

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
            "/items" : {
              "get" : {
                "summary" : "Get items with enum filters",
                "operationId" : "GetItems",
                "x-category" : "default",
                "parameters" : [ {
                  "name" : "status",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiEnumFilterParameterTest.MyFilterKeys",
                  "schema" : {
                    "$ref" : "#/components/schemas/OpenApiEnumFilterParameterTest.StatusEnum"
                  }
                }, {
                  "name" : "priority",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiEnumFilterParameterTest.MyFilterKeys",
                  "schema" : {
                    "$ref" : "#/components/schemas/OpenApiEnumFilterParameterTest.PriorityEnum"
                  }
                }, {
                  "name" : "scope",
                  "in" : "query",
                  "required" : false,
                  "description" : "Comma-separated list of filter keys that define the query scope. Scope filters affect totalElementsCount in pagination.",
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiEnumFilterParameterTest.MyFilterKeys",
                  "schema" : {
                    "type" : "string",
                    "enum" : [ "status", "priority" ],
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                } ],
                "responses" : {
                  "200" : {
                    "description" : "Success"
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
              "OpenApiEnumFilterParameterTest.StatusEnum" : {
                "type" : "string",
                "enum" : [ "Active", "Inactive", "Pending" ]
              },
              "OpenApiEnumFilterParameterTest.PriorityEnum" : {
                "type" : "string",
                "enum" : [ "Low", "Medium", "High" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testParameterStringWithEnum() {
    val builder = OpenApiRequestBuilder(emptyList())
    builder.parameterStringWithEnum(UrlParameterName("status"), listOf("active", "inactive", "pending"))

    assertThat(builder.parameters).hasSize(1)
    val parameter = builder.parameters[0]

    assertThat(parameter.name.value).isEqualTo("status")
    assertThat(parameter.description).isNull()

    val parameterType = parameter.type
    assertThat(parameterType).isNotNull()

    parameterType as SchemaDescriptor.InlineEnumSchema

    assertThat(parameterType.allowedValues).containsExactly("active", "inactive", "pending")
  }

  private enum class OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED
  }

  private enum class KotlinEnumFilterKeys(override val typedFilterKey: TypedFilterKey) : HasTypedFilterKey {
    Status(FilterKey("orderStatus").enum<OrderStatus>()),
  }

  @Test
  fun testKotlinEnumFilterParameters() {
    val route = openApiRoute("/orders", HttpMethod.Get, emptyList()) {
      summary = "Get orders with Kotlin enum filters"

      request {
        filteredBy(KotlinEnumFilterKeys.entries.toList(), KotlinEnumFilterKeys::class)
      }

      response("Success") {
        statusCode(HttpStatusCode.OK)
        noExamples()
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
      .generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
      .toString()

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
            "/orders" : {
              "get" : {
                "summary" : "Get orders with Kotlin enum filters",
                "operationId" : "GetOrders",
                "x-category" : "default",
                "parameters" : [ {
                  "name" : "orderStatus",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiEnumFilterParameterTest.KotlinEnumFilterKeys",
                  "schema" : {
                    "$ref" : "#/components/schemas/OpenApiEnumFilterParameterTest.OrderStatus"
                  }
                }, {
                  "name" : "scope",
                  "in" : "query",
                  "required" : false,
                  "description" : "Comma-separated list of filter keys that define the query scope. Scope filters affect totalElementsCount in pagination.",
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiEnumFilterParameterTest.KotlinEnumFilterKeys",
                  "schema" : {
                    "type" : "string",
                    "enum" : [ "orderStatus" ],
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                } ],
                "responses" : {
                  "200" : {
                    "description" : "Success"
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
              "OpenApiEnumFilterParameterTest.OrderStatus" : {
                "type" : "string",
                "enum" : [ "PENDING", "PROCESSING", "SHIPPED", "DELIVERED" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testFilterForEnum() {
    val builder = OpenApiRequestBuilder(listOf(UrlParameterName("the-parameter")))
    builder.filteredBy(ProductFilterKeys.entries.toList(), ProductFilterKeys::class)

    assertThat(builder.parameters).hasSize(5)

    assertThat(builder.parameters[0].name.value).isEqualTo("the-parameter")
    assertThat(builder.parameters[0].description).isNull()
    assertThat(builder.parameters[0].type).isInstanceOf(SchemaDescriptor.NativePrimitiveSchema::class)

    assertThat(builder.parameters[1].name.value).isEqualTo("productType")
    assertThat(builder.parameters[1].description).isNull()
    val schemaDescriptor = builder.parameters[1].type
    assertThat(schemaDescriptor).isInstanceOf(SchemaDescriptor.EnumSchema::class)

    schemaDescriptor as SchemaDescriptor.EnumSchema

    assertThat(schemaDescriptor.enumType.simpleNameWithEnclosing).isEqualTo("OpenApiEnumFilterParameterTest.ProductSortKey")
  }

  /**
   * Sort keys for product search
   */
  private enum class ProductSortKey : it.neckar.rest.RestResponseSorts {
    Name,
    Price,
    CreatedAt
  }

  /**
   * Filter keys for product search
   */
  private enum class ProductFilterKeys(override val typedFilterKey: TypedFilterKey) : HasTypedFilterKey {
    Type(FilterKey("productType").enum<ProductSortKey>()),
    Name(FilterKey("name").string()),
    MinPrice(FilterKey("minPrice").double()),
  }

  @Test
  fun testDiscriminatorEnumWithSearchParameters() {
    val route = openApiRoute("/products/search", HttpMethod.Get, emptyList()) {
      summary = "Search products with pagination, sorting, and filtering"

      request {
        paginated()
        sortedBy<ProductSortKey>()
        filteredBy(ProductFilterKeys.entries.toList(), ProductFilterKeys::class)
      }

      response("Success") {
        statusCode(HttpStatusCode.OK)
        noExamples()
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
      .generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
      .toString()

    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      """
        {
          "openapi" : "3.1.0",
          "info" : {
            "title" : "REST API",
            "description" : "REST API",
            "version" : "1.0.0"
          },
          "servers" : [ ],
          "paths" : {
            "/products/search" : {
              "get" : {
                "summary" : "Search products with pagination, sorting, and filtering",
                "operationId" : "GetSearchForProducts",
                "x-category" : "default",
                "parameters" : [ {
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
                }, {
                  "name" : "sortBy",
                  "in" : "query",
                  "required" : false,
                  "schema" : {
                    "${'$'}ref" : "#/components/schemas/OpenApiEnumFilterParameterTest.ProductSortKey"
                  }
                }, {
                  "name" : "sortOrder",
                  "in" : "query",
                  "required" : false,
                  "schema" : {
                    "${'$'}ref" : "#/components/schemas/Sort.Order"
                  }
                }, {
                  "name" : "productType",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiEnumFilterParameterTest.ProductFilterKeys",
                  "schema" : {
                    "${'$'}ref" : "#/components/schemas/OpenApiEnumFilterParameterTest.ProductSortKey"
                  }
                }, {
                  "name" : "name",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiEnumFilterParameterTest.ProductFilterKeys",
                  "schema" : {
                    "type" : "string",
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                }, {
                  "name" : "minPrice",
                  "in" : "query",
                  "required" : false,
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiEnumFilterParameterTest.ProductFilterKeys",
                  "schema" : {
                    "type" : "number",
                    "format" : "double",
                    "x-source-kotlin-type" : "kotlin.Double"
                  }
                }, {
                  "name" : "scope",
                  "in" : "query",
                  "required" : false,
                  "description" : "Comma-separated list of filter keys that define the query scope. Scope filters affect totalElementsCount in pagination.",
                  "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiEnumFilterParameterTest.ProductFilterKeys",
                  "schema" : {
                    "type" : "string",
                    "enum" : [ "productType", "name", "minPrice" ],
                    "x-source-kotlin-type" : "kotlin.String"
                  }
                } ],
                "responses" : {
                  "200" : {
                    "description" : "Success"
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
              "OpenApiEnumFilterParameterTest.ProductSortKey" : {
                "type" : "string",
                "enum" : [ "Name", "Price", "CreatedAt" ]
              },
              "Sort.Order" : {
                "type" : "string",
                "enum" : [ "Asc", "Desc" ]
              }
            }
          }
        }
      """.trimIndent()
    )
  }
}
