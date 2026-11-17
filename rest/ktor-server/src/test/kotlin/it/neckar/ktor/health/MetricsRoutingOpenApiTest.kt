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
package it.neckar.ktor.health

import io.ktor.server.testing.*
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.metricsRouting
import it.neckar.ktor.server.openapi.ktor.OpenApiPlugin
import it.neckar.ktor.server.openapi.ktor.openApiRoutes
import it.neckar.ktor.server.openapi.ktor.verifyOpenApiDocComplete
import it.neckar.open.http.Url
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.open.test.utils.LocalResourceGenerationSupport
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.OpenApiMetaData
import org.junit.jupiter.api.Test
import java.io.File
import it.neckar.open.test.utils.DisableLogging

@DisableLogging
class MetricsRoutingOpenApiTest {
  @Test
  fun testMetricsRoutingOpenApi() = testApplication {
    install(OpenApiPlugin) {
      metadata = OpenApiMetaData(
        title = "Test API",
        description = "Da Test API",
      )
      authenticationRequirement = AuthenticationRequirement.None
    }

    application {
      metricsRouting(
        baseUrl = Url.pattern("api/test-service")
      )
    }

    startApplication()
    application.verifyOpenApiDocComplete()

    val docs = application.openApiRoutes(OpenApiMetaData.Category.Metrics)

    val actualOpenApiJson = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.usingHarvestedSpecs()).generate(docs, additionalTypes = emptyList(), metaData = OpenApiMetaData.default).toStringPretty()

    LocalResourceGenerationSupport(
      target = File("build/generated/openapi/main/openapi-metrics-routing.json"),
    ).generate(actualOpenApiJson)
  }
}
