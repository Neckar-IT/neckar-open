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

import io.ktor.http.*
import kotlin.jvm.optionals.getOrNull
import it.neckar.rest.openapi.dsl.OpenApiRoute
import it.neckar.rest.openapi.dsl.OpenApiRequestBuilder
import it.neckar.rest.openapi.dsl.OpenApiResponseBuilder
import it.neckar.rest.openapi.dsl.OpenApiRouteBuilder
import it.neckar.rest.openapi.dsl.OpenApiSimpleBodyBuilder

/**
 * Thrown when the OpenAPI documentation is missing or incomplete.
 */
class OpenApiException(
  /**
   * The problem that occurred.
   */
  val problem: Problem,
  /**
   * The route that caused the exception, if available.
   */
  val path: String? = null,
  /**
   * The HTTP method that caused the exception, if available.
   */
  val method: HttpMethod? = null,

  /**
   * The source location of the problem, if available.
   */
  val sourceLocation: String? = guessOpenApiCallSourceLocation(),

  /**
   * The cause
   */
  cause: Throwable? = null,
) : Exception(problem.createMessage(path, method, sourceLocation), cause) {

  /**
   * The problem description
   */
  sealed interface Problem {
    /**
     * Creates a message for the problem, including the path and method if available.
     */
    fun createMessage(path: String?, method: HttpMethod?, sourceLocation: String?): String

    data object MissingExample : Problem {
      override fun createMessage(path: String?, method: HttpMethod?, sourceLocation: String?): String {
        return if (path != null) {
          "$method [$path] is missing an example. Call #noExamples() if no examples are relevant. $sourceLocation"
        } else {
          "No example provided. Call #noExamples() if no examples are relevant. $sourceLocation"
        }
      }
    }

    data object MissingResponseBody : Problem {
      override fun createMessage(path: String?, method: HttpMethod?, sourceLocation: String?): String {
        return if (path != null) {
          "$method [$path] is missing a response. Call #noResponseBody() if the endpoint does not return a response body. $sourceLocation"
        } else {
          "No response provided. Call #noResponseBody() if no response is relevant. $sourceLocation"
        }
      }
    }

    data class MissingDocumentation(val routes: List<UndocumentedRoute>) : Problem {
      init {
        require(routes.isNotEmpty()) { "Needs at least one undocumented route" }
      }

      override fun createMessage(path: String?, method: HttpMethod?, sourceLocation: String?): String {
        return routes
          .sortedBy { it.value }
          .joinToString(
            prefix = "Routes without open API documentation. Please add a 'doc{}' block to each:\n",
            separator = "\n",
          ) { "  $it" }
      }
    }

    /**
     * Carries every miscategorized route of one check, as [MissingDocumentation] does.
     */
    data class MiscategorizedRoutes(val routes: List<MiscategorizedRoute>) : Problem {
      init {
        require(routes.isNotEmpty()) { "Needs at least one miscategorized route" }
      }

      override fun createMessage(path: String?, method: HttpMethod?, sourceLocation: String?): String {
        return routes.joinToString(
          prefix = "Routes whose open API category contradicts their path. Set 'category' in the 'doc{}' block or move the route:\n",
          separator = "\n",
        ) { miscategorizedRoute ->
          val location = miscategorizedRoute.sourceInfo?.let { " (${it.location})" }.orEmpty()
          "  ${miscategorizedRoute.describe()}$location"
        }
      }
    }
  }
}

/**
 * Contains all class names that should be skipped when guessing the source location of the OpenAPI call.
 */
private val classNamesToSkip = setOf(
  OpenApiException::class,
  OpenApiRoute::class,
  OpenApiRouteBuilder::class,
  OpenApiSimpleBodyBuilder::class,
  OpenApiRequestBuilder::class,
  OpenApiResponseBuilder::class,
).flatMap {
  val qualifiedName = it.qualifiedName ?: throw IllegalStateException("Class ${it.simpleName} does not have a qualified name")
  listOf(qualifiedName, qualifiedName + "Kt")
}.toSet() + setOf("it.neckar.ktor.server.openapi.ktor.OpenApiKtorExtKt")

/**
 * Guesses the source location of the exception by inspecting the stack trace.
 */
fun guessOpenApiCallSourceLocation(): String {
  val found = Thread.currentThread().stackTrace
    .toList()
    .stream()
    .skip(1) // Skip the first element, which is this method
    .filter { classNamesToSkip.contains(it.className).not() }
    .findFirst()

  return found.getOrNull()?.toString() ?: "Unknown source location"
}
