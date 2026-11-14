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
package it.neckar.ktor.server.openapi.ktor

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import io.ktor.http.HttpMethod
import it.neckar.open.http.Url
import it.neckar.open.kotlin.serializers.JsonPath
import it.neckar.open.serialization.atPath
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.dsl.openApiRoute
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.transform.OpenApiDocumentTransformer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class OpenApiDocumentSourceTest {
  @Test
  fun `the base URL decides the generated operation ids`() {
    // Orval names the client functions after them, so the fingerprint has to see the served ids.
    val route = openApiRoute("/api/agents/jobs", HttpMethod.Get, emptyList()) {
      noResponseBody()
    }
    val source = OpenApiDocumentSource(baseUrl = Url.pattern("api/agents"), metadata = OpenApiMetaData.default, additionalTypes = OpenApiAdditionalTypes.empty, documentTransformers = OpenApiDocumentTransformer.Defaults)

    val document = source.generateCategory(category = OpenApiMetaData.Category.Default, routes = listOf(route)).jsonObject

    assertThat(document.atPath(JsonPath("paths", "/api/agents/jobs", "get"))["operationId"]!!.jsonPrimitive.content).isEqualTo("GetJobs")
  }

  @Test
  fun `additional schemas of a category reach its document with their examples and merge by plus`() {
    val category = OpenApiMetaData.Category("pdf")
    val withExample = OpenApiAdditionalTypes(
      additionalTypes = emptyMap(),
      additionalSchemas = mapOf(category to listOf(SchemaDescriptor.referenceObject<ExampleContext> { example("first", ExampleContext("a")) })),
    )
    val merged = OpenApiAdditionalTypes.environment(listOf(String::class)) + withExample
    val source = OpenApiDocumentSource(baseUrl = Url.pattern(""), metadata = OpenApiMetaData.default, additionalTypes = merged, documentTransformers = OpenApiDocumentTransformer.Defaults)

    val document = source.generateCategory(category = category, routes = emptyList()).jsonObject

    assertThat(source.categories(emptyList())).containsExactlyInAnyOrder(OpenApiMetaData.Category.Environment, category)
    val schema = document.atPath(JsonPath("components", "schemas", "OpenApiDocumentSourceTest.ExampleContext"))
    assertThat(schema["x-examples"]!!.jsonObject.keys).containsExactlyInAnyOrder("first")
  }

  @Serializable
  data class ExampleContext(val name: String)
}
