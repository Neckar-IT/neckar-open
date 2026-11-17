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

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import it.neckar.ktor.KtorService
import it.neckar.ktor.collectPathSegments
import it.neckar.ktor.collectRoutes
import it.neckar.ktor.collectSelectors
import it.neckar.open.collections.fastForEach
import it.neckar.open.exceptions.findCallerLocation
import it.neckar.open.http.UrlPathSegments
import it.neckar.rest.openapi.AdminRoutePathPrefix
import it.neckar.rest.openapi.MiscategorizedRoute
import it.neckar.rest.openapi.OpenApiException
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.UndocumentedRoute
import it.neckar.rest.openapi.documentation.KotlinSourceInfo
import it.neckar.rest.openapi.dsl.OpenApiRoute
import it.neckar.rest.openapi.dsl.OpenApiRouteBuilder
import it.neckar.rest.openapi.dsl.openApiRoute
import it.neckar.rest.openapi.isAdminRoutePath


/**
 * Adds the documentation for this route.
 *
 * The documentation can be retrieved using [Route.openApiRoute]
 */
fun Route.doc(block: OpenApiRouteBuilder.() -> Unit) {
  //Fetch the location
  val sourceInformation = KotlinSourceInfo.create(findCallerLocation())

  val selectors = collectSelectors()

  val method = selectors.filterIsInstance<HttpMethodRouteSelector>().firstOrNull()?.method ?: throw IllegalStateException("No HttpMethodRouteSelector found for route: $this. Call doc() on an endpoint that has a method (e.g., get, post, etc.)")

  val pathSegments: UrlPathSegments = selectors.collectPathSegments()
  val openApiRoute = openApiRoute(path = pathSegments, method = method, pathParameterNames = pathSegments.parameterNames(), sourceInformation = sourceInformation, block = block)
  attributes.put(OpenApiRouteAttribute, openApiRoute)
}

/**
 * Dumps the routes - including the documentation
 */
context(ktorService: KtorService)
fun Pipeline<*, PipelineCall>.dumpRoutesWithDoc(): String {
  val baseUrl = ktorService.baseUrl

  return buildString {
    this@dumpRoutesWithDoc.collectRoutes().fastForEach { route ->
      val openApiRoute = route.openApiRoute

      val routePath = baseUrl + route.toString()
      append(routePath)

      val requestType = openApiRoute?.request?.body?.type
      if (requestType != null) {
        append(" ($requestType)")
      }

      val returnType = openApiRoute?.bestResponse?.body?.type
      if (returnType != null) {
        append(": $returnType")
      }

      appendLine()
      openApiRoute?.description?.let { appendLine("    $it") }
    }
  }
}

/**
 * Returns the open api routes
 */
fun List<Route>.openApiRoutes(): List<OpenApiRoute> {
  return mapNotNull {
    it.openApiRoute
  }
}

/**
 * Returns the open api routes for the given category
 */
fun List<Route>.openApiRoutes(category: OpenApiMetaData.Category): List<OpenApiRoute> {
  return openApiRoutes()
    .filter {
      it.category == category
    }
}

/**
 * Returns the open api routes
 */
fun Pipeline<*, PipelineCall>.openApiRoutes(): List<OpenApiRoute> {
  return collectRoutes().openApiRoutes()
}

/**
 * Returns the open api routes for the given category
 */
fun Pipeline<*, PipelineCall>.openApiRoutes(category: OpenApiMetaData.Category): List<OpenApiRoute> {
  return collectRoutes().openApiRoutes(category)
}

/**
 * Verifies that all routes have open api documentation.
 * Throws an [OpenApiException] naming every route that is missing documentation.
 */
fun List<Route>.verifyOpenApiDocComplete() {
  val routesWithoutDoc: List<Route> = routesWithoutDoc()
  if (routesWithoutDoc.isEmpty()) {
    return
  }

  val undocumentedRoutes: List<UndocumentedRoute> = routesWithoutDoc.map { UndocumentedRoute(it.toString()) }

  throw OpenApiException(
    OpenApiException.Problem.MissingDocumentation(undocumentedRoutes),
    sourceLocation = null, //the message names every route, the location of this check says nothing about them
  )
}

/**
 * Verifies that all routes have open api documentation.
 * Throws an [OpenApiException] naming every route that is missing documentation.
 */
fun Pipeline<*, PipelineCall>.verifyOpenApiDocComplete() {
  collectRoutes().verifyOpenApiDocComplete()
}

/**
 * Verifies that path and category agree for every documented route: a route under [AdminRoutePathPrefix]
 * carries [OpenApiMetaData.Category.Admin], a route with that category lies under [AdminRoutePathPrefix].
 * Throws an [OpenApiException] naming every route where they contradict each other, ordered by path,
 * then method.
 *
 * A route without a `doc{}` block carries no category at all - [verifyOpenApiDocComplete] names it.
 */
fun List<Route>.verifyAdminRouteCategories() {
  val miscategorizedRoutes = openApiRoutes()
    .mapNotNull { it.findAdminCategoryMismatch() }
    .sortedWith(compareBy({ it.path.format() }, { it.method.value }))

  if (miscategorizedRoutes.isEmpty()) {
    return
  }

  throw OpenApiException(
    OpenApiException.Problem.MiscategorizedRoutes(miscategorizedRoutes),
    sourceLocation = null, //each route carries its own source location
  )
}

/**
 * Verifies that path and category agree for every documented route.
 * Throws an [OpenApiException] naming every route where they contradict each other.
 */
fun Pipeline<*, PipelineCall>.verifyAdminRouteCategories() {
  return collectRoutes().verifyAdminRouteCategories()
}

/**
 * Returns how path and category of this route contradict each other - null if they agree.
 */
private fun OpenApiRoute.findAdminCategoryMismatch(): MiscategorizedRoute? {
  val hasAdminPath = path.isAdminRoutePath()
  val hasAdminCategory = category == OpenApiMetaData.Category.Admin

  if (hasAdminPath == hasAdminCategory) {
    return null
  }

  return if (hasAdminPath) {
    MiscategorizedRoute.AdminPathWithoutAdminCategory(path, method, category, sourceInfo)
  } else {
    MiscategorizedRoute.AdminCategoryOutsideAdminPath(path, method, sourceInfo)
  }
}

/**
 * Returns all routes that do not have open api documentation.
 */
internal fun List<Route>.routesWithoutDoc(): List<Route> {
  return filter {
    it.openApiRoute == null
  }
}
