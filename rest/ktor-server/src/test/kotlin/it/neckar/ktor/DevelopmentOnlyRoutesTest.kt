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
package it.neckar.ktor

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import it.neckar.open.test.utils.DisableLogging
import it.neckar.runtime.context.DeploymentStage
import org.junit.jupiter.api.Test

@DisableLogging
class DevelopmentOnlyRoutesTest {

  @Test
  fun `development builds the routes`() = testApplication {
    routing {
      developmentOnly(DeploymentStage.Development) {
        get("/clear") { call.respondText("cleared") }
      }
    }

    val response = client.get("/clear")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(response.bodyAsText()).isEqualTo("cleared")
  }

  @Test
  fun `every other stage leaves the routes out`() {
    for (stage in DeploymentStage.entries.filter { it != DeploymentStage.Development }) {
      testApplication {
        routing {
          developmentOnly(stage) {
            get("/clear") { call.respondText("cleared") }
          }
          get("/health") { call.respondText("up") }
        }

        assertThat(client.get("/clear").status, "stage $stage").isEqualTo(HttpStatusCode.NotFound)
        assertThat(client.get("/health").status, "stage $stage").isEqualTo(HttpStatusCode.OK)
      }
    }
  }
}
