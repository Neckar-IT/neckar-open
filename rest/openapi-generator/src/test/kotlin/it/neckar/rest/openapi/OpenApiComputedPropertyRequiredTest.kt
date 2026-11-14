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
import assertk.assertions.isNotNull
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.neckar.open.kotlin.serializers.JsonPath
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.serialization.atPath
import it.neckar.open.serialization.requiredKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.openApiRoute

/**
 * Computed properties (defined in class body, not constructor) should be marked as
 * "required" in OpenAPI schema because they are always serialized.
 *
 * Problem: `isElementOptional` returns `true` for computed properties (they're optional
 * during deserialization because they get computed). But for serialization/OpenAPI response
 * schemas, they are always present and should be "required".
 */
class OpenApiComputedPropertyRequiredTest {

  /**
   * Minimal class with a computed property.
   * - `value` is a constructor parameter (required)
   * - `computed` is a computed property in the body (always serialized, but marked as optional by kotlinx.serialization)
   */
  @Serializable
  data class ClassWithComputedProperty(
    val value: Int,
  ) {
    /**
     * This computed property is always present in serialized output,
     * but `isElementOptional` returns `true` for it.
     */
    val computed: Int = value * 2
  }

  @Test
  fun `computed property should be marked as required in OpenAPI schema`() {
    val route = openApiRoute("/api/test", HttpMethod.Get, emptyList()) {
      response("Get test response") {
        statusCode = HttpStatusCode.OK
        body<ClassWithComputedProperty> {
          example("Example", ClassWithComputedProperty(5))
        }
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
      .generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
    val jsonString = json.toStringPretty()

    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val components = jsonObject.atPath(JsonPath("components", "schemas"))

    val schema = components.filterKeys { it.contains("ClassWithComputedProperty") }.values.first().jsonObject
    val properties = schema["properties"]!!.jsonObject

    // Verify both properties exist
    assertThat(properties["value"]).isNotNull()
    assertThat(properties["computed"]).isNotNull()

    // Constructor parameter and computed property (always serialized) must both be required.
    assertThat(schema).requiredKeys().contains("value")
    assertThat(schema).requiredKeys().contains("computed")
  }
}
