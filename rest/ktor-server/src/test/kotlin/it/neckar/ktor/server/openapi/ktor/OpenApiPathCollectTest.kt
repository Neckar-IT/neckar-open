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

import assertk.*
import assertk.assertions.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.ktor.collectAllRoutes
import it.neckar.ktor.get
import it.neckar.open.http.UrlParameterName
import it.neckar.open.http.UrlPattern
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.DisableLogging

/**
 *
 */
@DisableLogging
class OpenApiPathCollectTest {
  @Test
  fun testNullableCollection() = testApplication {
    val projectIdParameterName = UrlParameterName("projectId")
    val configurationIdParameterName = UrlParameterName("projectConfigurationId")
    val additionalInstallationsIdParameterName = UrlParameterName("additionalInstallationsIdParameterName")

    val pathPattern = UrlPattern.relative("/api/projects/") + projectIdParameterName + "configurations" + configurationIdParameterName + "additionalInstallations" + additionalInstallationsIdParameterName


    routing {
      get(pathPattern) {
        TODO()
      }.doc {
        noResponseBody()
      }
    }

    //The application will be available after startApplication() has been called
    lateinit var application: Application
    application {
      application = this
    }
    startApplication()


    assertThat(application).isNotNull()
    assertThat(application.attributes).isNotNull()

    val routes: List<Route> = application.collectAllRoutes()
    assertThat(routes).hasSize(9)

    val openApiRoutes = application.openApiRoutes()
    assertThat(openApiRoutes).hasSize(1)

    val openApiRoute = openApiRoutes.first()
    assertThat(openApiRoute.path.toString()).isEqualTo("/api/projects/{projectId}/configurations/{projectConfigurationId}/additionalInstallations/{additionalInstallationsIdParameterName}")
  }
}
