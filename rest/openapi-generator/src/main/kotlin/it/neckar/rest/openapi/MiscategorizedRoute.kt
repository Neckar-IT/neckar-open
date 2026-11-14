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

import io.ktor.http.HttpMethod
import it.neckar.open.http.UrlPathSegments
import it.neckar.rest.openapi.documentation.KotlinSourceInfo

/**
 * A route whose path and OpenAPI category contradict each other. Such a route is served by the spec
 * file of the category it declares, while a reader looks for it in the one its path implies.
 */
sealed interface MiscategorizedRoute {
  val path: UrlPathSegments

  val method: HttpMethod

  /**
   * Null for a route created without source information.
   */
  val sourceInfo: KotlinSourceInfo?

  /**
   * The route as it is named in the message.
   */
  val route: RouteDescription
    get() = RouteDescription("${method.value} ${path.format()}")

  /**
   * Names the route and how its path and its category contradict each other.
   */
  fun describe(): String

  /**
   * A route under [AdminRoutePathPrefix] that carries [category].
   */
  data class AdminPathWithoutAdminCategory(
    override val path: UrlPathSegments,
    override val method: HttpMethod,
    val category: OpenApiMetaData.Category,
    override val sourceInfo: KotlinSourceInfo?,
  ) : MiscategorizedRoute {
    init {
      require(category != OpenApiMetaData.Category.Admin) { "$route carries the admin category, so path and category agree" }
    }

    override fun describe(): String {
      return "$route: category '${category.value}', expected '${OpenApiMetaData.Category.Admin.value}' for a path under $AdminRoutePathPrefix"
    }
  }

  /**
   * A route outside [AdminRoutePathPrefix] that carries [OpenApiMetaData.Category.Admin].
   */
  data class AdminCategoryOutsideAdminPath(
    override val path: UrlPathSegments,
    override val method: HttpMethod,
    override val sourceInfo: KotlinSourceInfo?,
  ) : MiscategorizedRoute {
    override fun describe(): String {
      return "$route: category '${OpenApiMetaData.Category.Admin.value}', expected a path under $AdminRoutePathPrefix"
    }
  }
}
