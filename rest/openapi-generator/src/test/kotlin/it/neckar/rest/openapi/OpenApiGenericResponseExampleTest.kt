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
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.ktor.http.HttpMethod
import it.neckar.open.kotlin.serializers.JsonPath
import it.neckar.open.serialization.atPath
import it.neckar.rest.FoundResponse
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.openApiRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class OpenApiGenericResponseExampleTest {
  @Serializable
  private data class Item(val name: String)

  private val spec: JsonObject = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generate(
    listOf(
      openApiRoute("/items/{id}", HttpMethod.Get, emptyList()) {
        response("The item") {
          body<FoundResponse<Item>> {
            example("good", FoundResponse.good(Item("original")))
          }
        }
      },
      openApiRoute("/items/{id}/copy", HttpMethod.Get, emptyList()) {
        response("The copy") {
          body<FoundResponse<Item>> {
            example("good", FoundResponse.good(Item("copy")))
          }
        }
      },
      openApiRoute("/items/{id}/typed", HttpMethod.Get, emptyList()) {
        response("The item, its example typed as the variant") {
          body<FoundResponse<Item>> {
            example("good", FoundResponse.Good(Item("typed")))
          }
        }
      },
    ),
    additionalTypes = emptyList(),
    metaData = OpenApiMetaData.default,
  ).jsonObject

  private fun example(path: String, method: String, exampleId: String): JsonObject {
    return spec.atPath(JsonPath("paths", path, method, "responses", "200", "content", "application/json", "examples", exampleId))
  }

  @Test
  fun `good example names the variant in x-type-ref`() {
    val good = example("/items/{id}", "get", "good")

    assertThat(good.getValue(AdditionalOpenApiKeys.TypeRef).jsonPrimitive.content).isEqualTo("#/components/schemas/FoundResponse.Good__OpenApiGenericResponseExampleTest.Item__.WithType")
  }

  @Test
  fun `the referenced variant exists as component schema`() {
    val reference = example("/items/{id}", "get", "good").getValue(AdditionalOpenApiKeys.TypeRef).jsonPrimitive.content

    assertThat(spec.atPath(JsonPath("components", "schemas")).containsKey(reference.removePrefix("#/components/schemas/"))).isTrue()
  }

  @Test
  fun `two routes with the same generic response keep their own example`() {
    val original = example("/items/{id}", "get", "good")
    val copy = example("/items/{id}/copy", "get", "good")

    assertThat(copy.getValue(AdditionalOpenApiKeys.TypeRef)).isEqualTo(original.getValue(AdditionalOpenApiKeys.TypeRef))
    assertThat(original.getValue("value").toString()).isEqualTo("""{"data":{"name":"original"},"type":"good"}""")
    assertThat(copy.getValue("value").toString()).isEqualTo("""{"data":{"name":"copy"},"type":"good"}""")
  }

  @Test
  fun `an example typed as the variant names the same variant in x-type-ref`() {
    val typed = example("/items/{id}/typed", "get", "good")

    assertThat(typed.getValue(AdditionalOpenApiKeys.TypeRef)).isEqualTo(example("/items/{id}", "get", "good").getValue(AdditionalOpenApiKeys.TypeRef))
    assertThat(typed.getValue("value").toString()).isEqualTo("""{"data":{"name":"typed"},"type":"good"}""")
  }
}
