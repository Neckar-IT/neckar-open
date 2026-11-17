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

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.key
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.neckar.open.kotlin.serializers.JsonPath
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.serialization.atPath
import it.neckar.open.serialization.path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OpenApiHeaderBuilder
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.dsl.openApiRoute

/**
 * Tests for response headers generation to improve branch coverage.
 *
 * Covers the currently untested branch in generateResponse() in OpenApiJsonGenerator.kt.
 */
class OpenApiResponseHeadersTest {
  @Serializable
  private data class SimpleResponse(
    val message: String,
  )

  @Test
  fun testResponseWithSingleHeader() {
    val route = openApiRoute("/api/data", HttpMethod.Get, emptyList()) {
      response("Successful response with custom header") {
        statusCode = HttpStatusCode.OK
        body<SimpleResponse> {
          example("Example", SimpleResponse("Hello"))
        }
        header(
          OpenApiHeaderBuilder<String>("X-Custom-Header", SchemaDescriptor.NativePrimitiveSchema(typeOf<String>())).apply {
            description = "Custom header for tracking"
            required = true
          }
        )
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    val jsonString = json.toStringPretty()

    // Verify headers section exists in response
    assertThat(jsonString).contains("\"headers\"")
    assertThat(jsonString).contains("\"X-Custom-Header\"")
    assertThat(jsonString).contains("Custom header for tracking")

    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val headers = jsonObject.atPath(JsonPath("paths", "/api/data", "get", "responses", "200", "headers"))
    assertThat(headers["X-Custom-Header"]).isNotNull()

    val headerSchema = headers.atPath(JsonPath("X-Custom-Header", "schema"))
    assertThat(headerSchema["type"]!!.jsonPrimitive.content).isEqualTo("string")
  }

  @Test
  fun testResponseWithMultipleHeaders() {
    val route = openApiRoute("/api/export", HttpMethod.Get, emptyList()) {
      response("Export with multiple headers") {
        statusCode = HttpStatusCode.OK
        body<SimpleResponse> {
          example("Example", SimpleResponse("Export data"))
        }
        header(
          OpenApiHeaderBuilder<String>("X-Request-Id", SchemaDescriptor.NativePrimitiveSchema(typeOf<String>())).apply {
            description = "Unique request identifier"
            required = true
          }
        )
        header(
          OpenApiHeaderBuilder<Int>("X-Total-Count", SchemaDescriptor.NativePrimitiveSchema(typeOf<Int>())).apply {
            description = "Total number of items"
            required = false
          }
        )
        header(
          OpenApiHeaderBuilder<String>("X-Rate-Limit", SchemaDescriptor.NativePrimitiveSchema(typeOf<String>())).apply {
            description = "Rate limit information"
          }
        )
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    val jsonString = json.toStringPretty()

    // Verify all headers exist
    assertThat(jsonString).contains("\"X-Request-Id\"")
    assertThat(jsonString).contains("\"X-Total-Count\"")
    assertThat(jsonString).contains("\"X-Rate-Limit\"")
    assertThat(jsonString).contains("Unique request identifier")
    assertThat(jsonString).contains("Total number of items")
    assertThat(jsonString).contains("Rate limit information")

    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val headers = jsonObject.atPath(JsonPath("paths", "/api/export", "get", "responses", "200", "headers"))

    assertThat(headers.size).isEqualTo(3)
    assertThat(headers["X-Request-Id"]).isNotNull()
    assertThat(headers["X-Total-Count"]).isNotNull()
    assertThat(headers["X-Rate-Limit"]).isNotNull()

    // Verify header types
    val requestIdSchema = headers.atPath(JsonPath("X-Request-Id", "schema"))
    assertThat(requestIdSchema["type"]!!.jsonPrimitive.content).isEqualTo("string")

    val totalCountSchema = headers.atPath(JsonPath("X-Total-Count", "schema"))
    assertThat(totalCountSchema["type"]!!.jsonPrimitive.content).isEqualTo("integer")
  }

  @Test
  fun testResponseWithHeadersAndNoBody() {
    val route = openApiRoute("/api/ping", HttpMethod.Head, emptyList()) {
      response("Ping response with headers only") {
        statusCode = HttpStatusCode.OK
        body<Nothing>(SchemaDescriptor.EmptySchema) {
          noExamples()
        }
        header(
          OpenApiHeaderBuilder<String>("X-Server-Version", SchemaDescriptor.NativePrimitiveSchema(typeOf<String>())).apply {
            description = "Server version"
            example("Example", "1.0.0")
          }
        )
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    val jsonString = json.toStringPretty()

    // Verify header exists but no content section
    assertThat(jsonString).contains("\"X-Server-Version\"")
    assertThat(jsonString).contains("Server version")

    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val response = jsonObject.atPath(JsonPath("paths", "/api/ping", "head", "responses", "200"))

    assertThat(response["headers"]).isNotNull()
    val headers = response["headers"]!!.jsonObject
    assertThat(headers["X-Server-Version"]).isNotNull()

    val header = headers["X-Server-Version"]!!.jsonObject
    assertThat(header["schema"]).isNotNull()
    assertThat(header.atPath(JsonPath("examples", "Example"))["value"]!!.jsonPrimitive.content).isEqualTo("1.0.0")
  }

  @Test
  fun testResponseHeadersWithDifferentPrimitiveTypes() {
    val route = openApiRoute("/api/metrics", HttpMethod.Get, emptyList()) {
      response("Metrics with typed headers") {
        statusCode = HttpStatusCode.OK
        body<SimpleResponse> {
          example("Example", SimpleResponse("Metrics data"))
        }
        header(
          OpenApiHeaderBuilder<Int>("X-Page-Number", SchemaDescriptor.NativePrimitiveSchema(typeOf<Int>())).apply {
            description = "Current page number"
            example("Page1", 1)
          }
        )
        header(
          OpenApiHeaderBuilder<Long>("X-Timestamp", SchemaDescriptor.NativePrimitiveSchema(typeOf<Long>())).apply {
            description = "Response timestamp"
            example("Now", 1737363600000L)
          }
        )
        header(
          OpenApiHeaderBuilder<Boolean>("X-Has-More", SchemaDescriptor.NativePrimitiveSchema(typeOf<Boolean>())).apply {
            description = "Indicates if more data is available"
            example("True", true)
          }
        )
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    val jsonString = json.toStringPretty()

    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val headers = jsonObject.atPath(JsonPath("paths", "/api/metrics", "get", "responses", "200", "headers"))

    // Verify Int type
    val pageNumberSchema = headers.atPath(JsonPath("X-Page-Number", "schema"))
    assertThat(pageNumberSchema["type"]!!.jsonPrimitive.content).isEqualTo("integer")
    if (pageNumberSchema.containsKey("format")) {
      assertThat(pageNumberSchema["format"]!!.jsonPrimitive.content).isEqualTo("int32")
    }

    // Verify Long type
    val timestampSchema = headers.atPath(JsonPath("X-Timestamp", "schema"))
    assertThat(timestampSchema["type"]!!.jsonPrimitive.content).isEqualTo("integer")
    if (timestampSchema.containsKey("format")) {
      assertThat(timestampSchema["format"]!!.jsonPrimitive.content).isEqualTo("int64")
    }

    // Verify Boolean type
    val hasMoreSchema = headers.atPath(JsonPath("X-Has-More", "schema"))
    assertThat(hasMoreSchema["type"]!!.jsonPrimitive.content).isEqualTo("boolean")

    // Verify schemas exist (examples are optional)
    assertThat(headers).path("X-Page-Number").key("schema").isNotNull()
    assertThat(headers).path("X-Timestamp").key("schema").isNotNull()
    assertThat(headers).path("X-Has-More").key("schema").isNotNull()
  }
}
