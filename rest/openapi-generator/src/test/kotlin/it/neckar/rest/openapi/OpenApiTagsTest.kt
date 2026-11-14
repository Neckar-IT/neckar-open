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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.neckar.open.http.Url
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.openApiRoute
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

/**
 * A generator created via [OpenApiJsonGenerator.withTags] derives a tag from each route's first
 * path segment (after the base path) and groups the tags of a category into an `x-tagGroups` entry.
 */
class OpenApiTagsTest {
  private val generator = OpenApiJsonGenerator.withTags(
    basePath = Url.relative("api"),
    documentationProvider = DocumentationProvider.None,
  )

  private fun getRoute(path: String) = openApiRoute(path, HttpMethod.Get, emptyList()) {
    response("ok") {
      statusCode = HttpStatusCode.OK
      noExamples()
    }
  }

  @Test
  fun `tags are derived from the first path segment and grouped by category`() {
    val spec = generator.generate(
      routes = listOf(getRoute("/api/books"), getRoute("/api/authors")),
      metaData = OpenApiMetaData.default,
    ).jsonObject

    // one tag per distinct first path segment, alphabetically sorted
    assertThat(spec.getValue("tags")).isJsonEqualTo(
      //language=JSON
      """
      [
        { "name": "authors", "x-displayName": "Authors" },
        { "name": "books", "x-displayName": "Books" }
      ]
      """.trimIndent()
    )

    // both tags live in the default category, so they share one "API" group
    assertThat(spec.getValue("x-tagGroups")).isJsonEqualTo(
      //language=JSON
      """[ { "name": "API", "tags": ["authors", "books"] } ]"""
    )
  }
}
