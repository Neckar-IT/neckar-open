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

import it.neckar.open.annotations.Effective
import it.neckar.open.http.Url
import it.neckar.open.http.UrlParameterName
import it.neckar.open.http.UrlPattern
import it.neckar.rest.openapi.OpenApiMetaData

/**
 * Endpoints for the OpenAPI documentation
 *
 * Execute `OpenApiEndpointsEffectiveTest` to update the @Effective annotations
 */
object OpenApiEndpoints {
  object Parameters {
    val category: UrlParameterName = UrlParameterName("category")
  }

  /**
   * Root of every OpenAPI documentation route — the JSON specs and the Scalar UIs below all live
   * under this segment.
   */
  @Effective("doc")
  val DocRoot: UrlPattern.Relative0 = Url.pattern("doc")

  /**
   * Root-relative URL of the whole documentation route tree, including the service's [baseUrl] —
   * e.g. `/api/patterns/doc`. For addressing the tree as a whole, such as exempting it from a
   * Content Security Policy.
   */
  fun docRootFullUrl(baseUrl: UrlPattern.Relative0): Url.RootRelative =
    Url.root + baseUrl.resolve() + DocRoot.resolve()

  @Effective("doc/openapi")
  val Openapi: UrlPattern.Relative0 = DocRoot + "openapi"
  fun openapiUrl(): Url.Relative = Openapi.resolve()

  /**
   * Root-relative URL for the full OpenAPI JSON spec, including the service's [baseUrl].
   * Useful for building absolute references (e.g. the `data-url` attribute of the Scalar UI).
   */
  fun openapiFullUrl(baseUrl: UrlPattern.Relative0): Url.RootRelative =
    Url.root + baseUrl.resolve() + openapiUrl()

  @Effective("doc/openapi/{category}")
  val ByCategory: UrlPattern.Relative1 = Openapi + Parameters.category
  fun byCategoryUrl(category: OpenApiMetaData.Category): Url.Relative = ByCategory.resolve(category.value)

  /**
   * Root-relative URL for the category-filtered OpenAPI JSON spec, including the service's [baseUrl].
   */
  fun byCategoryFullUrl(baseUrl: UrlPattern.Relative0, category: OpenApiMetaData.Category): Url.RootRelative =
    Url.root + baseUrl.resolve() + byCategoryUrl(category)

  /**
   * OpenAPI JSON spec variant that auto-detects the `servers` list from the
   * incoming request's origin (X-Forwarded-* or direct connection). Falls back
   * to the configured servers when no forwarding headers are present.
   */
  @Effective("doc/openapi/with-origin")
  val OpenapiWithOrigin: UrlPattern.Relative0 = Openapi + "with-origin"
  fun openapiWithOriginUrl(): Url.Relative = OpenapiWithOrigin.resolve()

  /**
   * Root-relative URL for the live (auto-detected) OpenAPI JSON spec,
   * including the service's [baseUrl].
   */
  fun openapiWithOriginFullUrl(baseUrl: UrlPattern.Relative0): Url.RootRelative =
    Url.root + baseUrl.resolve() + openapiWithOriginUrl()

  @Effective("doc/openapi/categories")
  val Categories: UrlPattern.Relative0 = Openapi + "categories"
  fun categoriesUrl(): Url.Relative = Categories.resolve()

  /**
   * HTML UI (Scalar) rendering the full OpenAPI spec.
   */
  @Effective("doc/api")
  val Api: UrlPattern.Relative0 = DocRoot + "api"
  fun apiUrl(): Url.Relative = Api.resolve()

  /**
   * HTML UI (Scalar) rendering the OpenAPI spec for a single category.
   */
  @Effective("doc/api/{category}")
  val ApiByCategory: UrlPattern.Relative1 = Api + Parameters.category
  fun apiByCategoryUrl(category: OpenApiMetaData.Category): Url.Relative = ApiByCategory.resolve(category.value)
}