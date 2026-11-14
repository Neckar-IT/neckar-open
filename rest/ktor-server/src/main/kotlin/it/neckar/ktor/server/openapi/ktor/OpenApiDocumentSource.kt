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

import io.ktor.util.*
import it.neckar.open.http.UrlPattern
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.OpenApiRoute
import it.neckar.rest.openapi.transform.OpenApiDocumentTransformer
import kotlinx.serialization.json.JsonElement

/**
 * What [OpenApiPlugin] generates its documents from; its category documents and [protocolFingerprint] come from [generateCategory].
 */
internal class OpenApiDocumentSource(
  private val baseUrl: UrlPattern.Relative0,
  /**
   * The metadata of the untagged documents.
   */
  private val metadata: OpenApiMetaData,
  private val additionalTypes: OpenApiAdditionalTypes,
  private val documentTransformers: List<OpenApiDocumentTransformer>,
) {
  /**
   * Parses the harvested spec files once, on the first document.
   */
  private val documentationProvider: DocumentationProvider by lazy { DocumentationProvider.usingHarvestedSpecs() }

  /**
   * The generator for the untagged documents: [baseUrl] decides the generated operation ids.
   */
  private fun generator(): OpenApiJsonGenerator {
    return OpenApiJsonGenerator(basePath = baseUrl.resolve(), documentationProvider = documentationProvider, documentTransformers = documentTransformers)
  }

  /**
   * The generator for the tagged document Scalar renders.
   */
  fun taggedGenerator(): OpenApiJsonGenerator {
    return OpenApiJsonGenerator.withTags(basePath = baseUrl.resolve(), documentationProvider = documentationProvider, documentTransformers = documentTransformers)
  }

  /**
   * The document of every route, without additional types.
   */
  fun generateAll(routes: List<OpenApiRoute>): JsonElement {
    return generator().generate(routes = routes, additionalTypes = emptyList(), metaData = metadata)
  }

  /**
   * The categories a document is served for: those of [additionalTypes], then those of [routes].
   */
  fun categories(routes: List<OpenApiRoute>): Set<OpenApiMetaData.Category> {
    return additionalTypes.categories() + routes.map { it.category }
  }

  /**
   * The document of [category]: its routes and the additional types registered for it. Orval generates one client file from it.
   */
  fun generateCategory(category: OpenApiMetaData.Category, routes: List<OpenApiRoute>): JsonElement {
    return generator().generate(
      routes = routes.filter { it.category == category },
      additionalTypes = additionalTypes[category],
      metaData = metadata,
    )
  }
}

/**
 * The document source [OpenApiPlugin] was configured with.
 */
internal val OpenApiDocumentSourceKey: AttributeKey<OpenApiDocumentSource> = AttributeKey("OpenApiDocumentSource")
