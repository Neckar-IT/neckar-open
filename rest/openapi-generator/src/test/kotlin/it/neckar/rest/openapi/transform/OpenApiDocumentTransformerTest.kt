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
package it.neckar.rest.openapi.transform

import assertk.*
import assertk.assertions.*
import it.neckar.open.http.Url
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.documentation.DocumentationProvider
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class OpenApiDocumentTransformerTest {
  @Test
  fun `the generator applies the transformers in order`() {
    val generator = OpenApiJsonGenerator(
      documentationProvider = DocumentationProvider.None,
      documentTransformers = listOf(appendingToTitle("first"), appendingToTitle("second")),
    )

    val document = generator.generate(routes = emptyList(), metaData = OpenApiMetaData.default).jsonObject

    assertThat(document.getValue("info").jsonObject.getValue("title").jsonPrimitive.content).endsWith(" first second")
  }

  @Test
  fun `both factories hand the transformers to the generator`() {
    val transformers = listOf(appendingToTitle("marked"))

    listOf(
      OpenApiJsonGenerator(basePath = Url.relative(""), documentationProvider = DocumentationProvider.None, documentTransformers = transformers),
      OpenApiJsonGenerator.withTags(basePath = Url.relative(""), documentationProvider = DocumentationProvider.None, documentTransformers = transformers),
    ).forEach { generator ->
      val document = generator.generate(routes = emptyList(), metaData = OpenApiMetaData.default).jsonObject
      assertThat(document.getValue("info").jsonObject.getValue("title").jsonPrimitive.content).endsWith(" marked")
    }
  }

  private fun appendingToTitle(suffix: String): OpenApiDocumentTransformer {
    return OpenApiDocumentTransformer { document ->
      val info = document.json.getValue("info").jsonObject
      val title = info.getValue("title").jsonPrimitive.content
      OpenApiDocument(JsonObject(document.json + ("info" to JsonObject(info + ("title" to JsonPrimitive("$title $suffix"))))))
    }
  }
}
