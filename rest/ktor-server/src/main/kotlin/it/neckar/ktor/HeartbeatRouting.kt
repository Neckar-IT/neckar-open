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

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.neckar.ktor.server.openapi.ktor.doc
import it.neckar.open.http.UrlPattern
import it.neckar.rest.openapi.OpenApiMetaData.Category
import it.neckar.rest.openapi.dsl.OperationId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Contains the heartbeat routing.
 */
class HeartbeatRouting(
  /**
   * The base url for the meta-routing of the service.
   * E.g., `api/service-name`
   */
  val baseUrl: UrlPattern.Relative
) : KtorRoutes {
  override fun Application.configure() {
    routing {
      route(baseUrl + "heartbeat") {
        head {
          extractFeatureFlags().handle()
          call.respond<HttpStatusCode>(HttpStatusCode.OK)
        }.doc {
          category = Category.Heartbeat
          operationId = OperationId("HeadHeartbeat")

          summary = "Check if the service is running"
          description = "Returns a simple message to check if the service is running"
          response("A simple message") {
            statusCode = HttpStatusCode.OK
            noExamples()
          }
        }

        get {
          extractFeatureFlags().handle()
          call.respond<String>(ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
        }.doc {
          category = Category.Heartbeat
          operationId = OperationId("GetHeartbeat")

          summary = "Check if the service is running"
          description = "Returns a simple message to check if the service is running"
          response("A simple message") {
            statusCode = HttpStatusCode.OK
            body<String> {
              description = "The current date and time in ISO format"
              example("time", "2025-06-13T14:32:37.317829554Z[GMT]")
            }
          }
        }
      }
    }
  }
}
