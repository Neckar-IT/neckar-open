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
@file:Suppress("IdentifierGrammar")

package it.neckar.rest.openapi

import it.neckar.lifeCycle.ArchiveState
import it.neckar.open.annotations.SamePath
import it.neckar.open.collections.fastForEach
import it.neckar.open.kotlin.serializers.putNotNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import it.neckar.rest.openapi.dsl.OpenApiRoute
import it.neckar.rest.openapi.dsl.OperationId
import it.neckar.rest.openapi.schema.isNothing

/**
 * Helper class for path/route generation in OpenAPI.
 *
 * This class handles:
 * - Generation of the paths section
 * - Route grouping by path
 * - Route details generation (parameters, request body, responses)
 */
internal class OpenApiPathGenerator(
  private val schemaGenerator: OpenApiSchemaGenerator,
  private val responseGenerator: OpenApiResponseGenerator,
  private val operationIdProvider: (OpenApiRoute) -> OperationId?,
  private val tagProvider: (OpenApiRoute) -> OpenApiTag?,
) {

  /**
   * Generates the paths section of the OpenAPI spec.
   * Groups routes by their path and generates entries for each.
   */
  internal fun generatePathsSection(allRoutes: List<OpenApiRoute>): JsonObject {
    return buildJsonObject {
      //Group the routes by their path
      allRoutes.groupBy { it.path }
        .forEach { (path, routesForPath) ->
          //Generate the entry for the path - containing several routes
          put(path.format(), generatePathEntry(routesForPath))
        }
    }
  }

  /**
   * Generates the entries for one route - with different methods
   */
  fun generatePathEntry(
    /**
     * Contains all routes for the same path - which differ in their HTTP method.
     */
    @SamePath routesForSamePath: List<OpenApiRoute>,
  ): JsonElement {
    require(routesForSamePath.isNotEmpty()) { "No routes  provided" }

    //Ensure the same path
    routesForSamePath.map { it.path }.distinct().let {
      if (it.size != 1) {
        throw IllegalStateException("Multiple paths found: $it")
      }
    }

    return buildJsonObject {
      routesForSamePath.fastForEach { route ->
        put(route.method.value.lowercase(), generateRouteDetails(route))
      }
    }
  }

  /**
   * Generates the route details - for a single method inside a path
   */
  fun generateRouteDetails(route: OpenApiRoute): JsonElement {
    return buildJsonObject {
      putNotNull("description", route.description)
      putNotNull("summary", route.summary)
      putNotNull("operationId", operationIdProvider(route)?.value)

      val tag = tagProvider(route)
      if (tag != null) {
        putJsonArray("tags") {
          add(tag.name)
        }
      }

      put(AdditionalOpenApiKeys.Category, route.category.value)
      //An archived route is exposed as deprecated in the spec
      if (route.archiveState != ArchiveState.Active) {
        put("deprecated", true)
      }
      route.sourceInfo?.let {
        put(AdditionalOpenApiKeys.SourceRef.SourceInfo, buildJsonObject {
          put("className", it.className)
          put("methodName", it.methodName)
          put("location", it.location)
        })
      }

      put("parameters", buildJsonArray {
        route.request.parameters.fastForEach { parameter ->
          add(buildJsonObject {
            put("name", parameter.name.value)
            put("in", parameter.location.name.lowercase())
            put("required", parameter.required ?: false)
            parameter.description?.let {
              put("description", it)
            }

            parameter.kotlinSourceType?.let {
              put(AdditionalOpenApiKeys.SourceRef.KotlinType, it.qualifiedName)
            }

            //An archived parameter is exposed as deprecated in the spec
            if (parameter.archiveState != ArchiveState.Active) {
              put("deprecated", true)
            }

            schemaGenerator.putSchemaTypeOrReference(parameter.type)

            putExampleObjects(parameter.examples)
          })
        }
      })

      route.request.body?.let {
        if (it.type.isNothing().not()) {
          put("requestBody", responseGenerator.generateRequestBody(route.request))
        }
      }

      //Generate the responses
      put("responses", responseGenerator.generateResponses(route.responses))
    }
  }
}
