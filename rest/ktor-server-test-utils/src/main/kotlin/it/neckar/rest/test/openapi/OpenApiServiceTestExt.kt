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
package it.neckar.rest.test.openapi

import assertk.*
import assertk.assertions.isNotEmpty
import io.ktor.server.testing.*
import it.neckar.ktor.KtorService
import it.neckar.ktor.jwt.JwtConfiguration
import it.neckar.ktor.client.bodyAsTextSuccess
import it.neckar.ktor.client.get
import it.neckar.ktor.server.openapi.ktor.OpenApiEndpoints
import it.neckar.ktor.server.openapi.ktor.message.OpenApiCategoriesResponse
import it.neckar.ktor.server.openapi.ktor.verifyAdminRouteCategories
import it.neckar.ktor.server.openapi.ktor.verifyOpenApiDocComplete
import it.neckar.open.http.Url
import it.neckar.open.test.utils.LocalResourceGenerationSupport
import it.neckar.open.test.utils.isJsonEqualTo
import it.neckar.open.test.utils.value
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.test.TestClient
import it.neckar.rest.test.assertData
import it.neckar.rest.test.createTestClient
import it.neckar.rest.test.isGood
import it.neckar.rest.test.isHttpStatusSuccess
import it.neckar.rest.test.launchServiceInTest
import java.io.File

/**
 * Fetches the OpenAPI JSON of [service] for [category] and asserts the doc endpoint served a non-empty document.
 */
suspend fun TestClient.testOpenApi(service: KtorService, category: OpenApiMetaData.Category) {
  testOpenApi(service.serviceDescriptor, category)
}

/**
 * Fetches the OpenAPI JSON of [service] for [category] and asserts it equals [expectedJson].
 */
suspend fun TestClient.testOpenApi(service: KtorService, category: OpenApiMetaData.Category, expectedJson: () -> String) {
  testOpenApi(service.serviceDescriptor, category, expectedJson)
}

/**
 * Fetches the OpenAPI JSON of [serviceDescriptor] for [category] and asserts the doc endpoint served a non-empty document.
 */
suspend fun TestClient.testOpenApi(serviceDescriptor: ServiceDescriptor, category: OpenApiMetaData.Category) {
  assertThat(getOpenApiJson(serviceDescriptor, category)).isNotEmpty()
}

/**
 * Fetches the OpenAPI JSON of [serviceDescriptor] for [category] and asserts it equals [expectedJson].
 */
suspend fun TestClient.testOpenApi(serviceDescriptor: ServiceDescriptor, category: OpenApiMetaData.Category, expectedJson: () -> String) {
  assertThat(getOpenApiJson(serviceDescriptor, category)).isJsonEqualTo(expectedJson())
}

/**
 * Returns the service descriptor's OpenAPI JSON
 */
suspend fun TestClient.getOpenApiJson(serviceDescriptor: ServiceDescriptor, category: OpenApiMetaData.Category): String {
  val url = Url.relative(serviceDescriptor.apiPrefix) + OpenApiEndpoints.byCategoryUrl(category)

  val response = httpClient.get(Url.localhostHttp + url)
  assertThat(response).isHttpStatusSuccess()

  val json = response.bodyAsTextSuccess()
  return json
}

/**
 * Helper method for code completion
 */
suspend inline fun KtorService.testOpenApi(client: TestClient, category: OpenApiMetaData.Category) {
  client.testOpenApi(this, category)
}

/**
 * Writes the open api JSON file of every category [service] serves.
 *
 * Fails for a route of this test application without a `doc{}` block; a route that only the
 * production wiring of [service] registers stays unseen.
 *
 * Fails for a route of [service] whose category contradicts its path.
 */
suspend fun ApplicationTestBuilder.generateOpenApiJsonFiles(
  service: KtorService,
  sourceDirectory: String = "main",
  /**
   * The JWT configuration of a service whose doc endpoints require authentication
   * ([it.neckar.ktor.AuthenticationRequirement.Required]). When set, the spec-harvesting
   * client authenticates with a token minted from it. Services whose doc endpoints are public
   * ([it.neckar.ktor.AuthenticationRequirement.None]) leave it null.
   */
  jwtConfiguration: JwtConfiguration? = null,
) {
  launchServiceInTest(service)

  //The routing tree exists once the application is started - launchServiceInTest only registers the module
  startApplication()
  application.verifyOpenApiDocComplete()
  application.verifyAdminRouteCategories()

  val client = createTestClient(jwtConfiguration = jwtConfiguration)

  val categories = client.get<OpenApiCategoriesResponse>(Url.localhostHttp + service.serviceDescriptor.apiPrefix + OpenApiEndpoints.categoriesUrl())
    .isGood()
    .assertData()
    .value()

  val openapiJsonParentDir = File("build/generated/openapi/$sourceDirectory")

  categories.forEach { category ->
    val openApiJson = client.getOpenApiJson(service.serviceDescriptor, category)

    val fileName = "openapi-${service.serviceDescriptor.serviceName}-${category.value}.json"
    LocalResourceGenerationSupport(File(openapiJsonParentDir, fileName)).generate(openApiJson)
  }
}
