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
package it.neckar.ktor.mcp

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Test

class McpToolArgumentsTest {
  private val schema: ToolSchema = ToolSchema(
    properties = buildJsonObject {
      putJsonObject("query") {}
      putJsonObject("limit") {}
    },
  )

  @Test
  fun `a request without arguments has none`() {
    assertThat(request(arguments = null).strictArguments("search", schema)).isEqualTo(JsonObject(emptyMap()))
  }

  @Test
  fun `an argument outside the schema is refused`() {
    assertFailure { request(JsonObject(mapOf("qurey" to JsonPrimitive("x")))).strictArguments("search", schema) }
      .hasMessage("search takes only [query, limit] but got [qurey]")
  }

  @Test
  fun `a schema without properties takes no argument`() {
    assertFailure { request(JsonObject(mapOf("x" to JsonPrimitive(1)))).strictArguments("whoami", ToolSchema()) }
      .hasMessage("whoami takes only [] but got [x]")
  }

  @Test
  fun `a string argument is read, every other JSON value refused`() {
    assertThat(JsonObject(emptyMap()).optionalNonBlankString("query")).isNull()
    assertThat(JsonObject(mapOf("query" to JsonPrimitive("Anna"))).optionalNonBlankString("query")).isEqualTo("Anna")
    listOf(
      JsonPrimitive(42) to "a number",
      JsonPrimitive(" ") to "a blank string",
      JsonNull to "null",
      JsonObject(emptyMap()) to "an object",
    ).forEach { (value, found) ->
      assertFailure { JsonObject(mapOf("query" to value)).optionalNonBlankString("query") }
        .hasMessage("query must be a non-blank string, found $found")
    }
  }

  @Test
  fun `a required string argument is read, a missing one refused`() {
    assertThat(JsonObject(mapOf("query" to JsonPrimitive("Anna"))).requiredNonBlankString("query")).isEqualTo("Anna")
    assertFailure { JsonObject(emptyMap()).requiredNonBlankString("query") }.hasMessage("query is required")
  }

  @Test
  fun `a whole number within the range is read, every other JSON value refused`() {
    assertThat(JsonObject(emptyMap()).optionalInt("limit", 1..100)).isNull()
    assertThat(JsonObject(mapOf("limit" to JsonPrimitive(1))).optionalInt("limit", 1..100)).isEqualTo(1)
    assertThat(JsonObject(mapOf("limit" to JsonPrimitive(100))).optionalInt("limit", 1..100)).isEqualTo(100)
    listOf(
      JsonPrimitive(0) to "a whole number outside the range",
      JsonPrimitive(101) to "a whole number outside the range",
      JsonPrimitive(3_000_000_000) to "a number with a fraction or beyond Int",
      JsonPrimitive(5.5) to "a number with a fraction or beyond Int",
      JsonPrimitive("5") to "a string",
      JsonPrimitive(true) to "a boolean",
      JsonNull to "null",
    ).forEach { (value, found) ->
      assertFailure { JsonObject(mapOf("limit" to value)).optionalInt("limit", 1..100) }
        .hasMessage("limit must be a whole number between 1 and 100, found $found")
    }
  }

  private fun request(arguments: JsonObject?): CallToolRequest {
    return CallToolRequest(CallToolRequestParams(name = "search", arguments = arguments))
  }
}
