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

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.neckar.ktor.KtorRoutes
import it.neckar.ktor.get
import it.neckar.ktor.server.openapi.ktor.doc
import it.neckar.open.http.UrlPattern
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.dsl.OperationId

/**
 * Adds an endpoint for metrics
 */
class MetricsRouting(
  /**
   * The base URL - "metrics" will be appended to this
   */
  val baseUrl: UrlPattern.Relative,

  /**
   * The metrics provider
   */
  val metricsProvider: () -> String,
) : KtorRoutes {

  override fun Application.configure() {
    routing {
      get(baseUrl + "metrics") {
        call.respond<String>(metricsProvider())
      }.doc {
        category = OpenApiMetaData.Category.Metrics
        operationId = OperationId("GetMetrics")

        summary = "Returns the metrics"
        description = "Returns the metrics for the service in a format that can be processed by Prometheus."

        response("Metrics") {
          body<String> {
            noExamples()
          }
        }
      }

      get(baseUrl + "jvm/start-parameters") {
        call.respond<JvmStartupParameters>(JvmStartupParameters.create())
      }.doc {
        category = OpenApiMetaData.Category.Metrics
        operationId = OperationId("GetJvmStartupParameters")

        summary = "Returns the JVM startup parameters"
        description = "Returns the JVM startup parameters for the service."

        response("JVM Startup Parameters") {
          body<JvmStartupParameters> {
            noExamples()
          }
        }
      }

      get(baseUrl + "jvm/memory") {
        call.respond<JvmMemoryUsage>(JvmMemoryUsage.create())
      }.doc {
        category = OpenApiMetaData.Category.Metrics
        operationId = OperationId("GetJvmMemoryUsage")

        summary = "Returns the JVM memory usage"
        description = "Returns the JVM memory usage for the service."

        response("JVM Memory Usage") {
          body<JvmMemoryUsage> {
            noExamples()
          }
        }
      }
    }
  }
}
