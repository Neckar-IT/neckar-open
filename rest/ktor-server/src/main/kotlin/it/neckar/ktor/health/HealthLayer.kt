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

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import it.neckar.ktor.configure
import it.neckar.open.health.DependencyMonitor
import it.neckar.open.health.DependencyProbe
import it.neckar.open.health.HealthHttpStatus
import it.neckar.open.health.ServiceStateHolder
import it.neckar.open.health.error.GlobalExceptionCollector
import it.neckar.open.health.error.fromUncaughtExceptions
import it.neckar.open.health.from
import it.neckar.open.http.UrlPattern
import it.neckar.rest.error.GlobalHealth

/**
 * Holds the [ServiceStateHolder] in the application so the gating plugin and the routes share one
 * source of truth.
 */
val ServiceStateHolderAttribute: AttributeKey<ServiceStateHolder> = AttributeKey("ServiceStateHolder")

/**
 * Associates the [holder] with this application.
 */
fun Application.putServiceStateHolder(holder: ServiceStateHolder) {
  attributes.put(ServiceStateHolderAttribute, holder)
}

/**
 * Returns the [ServiceStateHolder] of this application, or `null` if the health layer is not installed.
 */
fun Application.findServiceStateHolder(): ServiceStateHolder? = attributes.getOrNull(ServiceStateHolderAttribute)

/**
 * Installs the shared health layer: the readiness gating on the routing root plus the
 * `/health/live`, `/health/ready`, `/health`, `/health/dependencies` routes.
 *
 * Every Ktor service with a database dependency calls this once in `Application.configure()`.
 *
 * The [DependencyMonitor] is constructed here instead of being passed in, so it always updates the
 * same [stateHolder] the routes and the gating plugin read.
 *
 * The process-wide [GlobalExceptionCollector] is connected to [stateHolder], so `/health` reports
 * the recorded uncaught exceptions. Exceptions recorded before this call, such as those of work done
 * before the server starts, are adopted. The subscription is removed when the application stops.
 *
 * With [DependencyPolling.Periodic] and at least one probe, the [DependencyMonitor] polls the probes until the
 * application stops.
 */
fun Application.installHealthLayer(
  /**
   * The service base URL the health routes are bound to (e.g. `api/widget`).
   */
  baseUrl: UrlPattern.Relative0,
  /**
   * The single source of truth for the service state.
   */
  stateHolder: ServiceStateHolder,
  /**
   * The external-dependency probes (MongoDb, S3, …).
   */
  probes: List<DependencyProbe>,
  dependencyPolling: DependencyPolling = DependencyPolling.Periodic,
) {
  val dependencyMonitor = DependencyMonitor(stateHolder, probes)
  putServiceStateHolder(stateHolder)
  routing {
    install(ReadinessGatingPlugin) {
      this.stateHolder = stateHolder
    }
  }
  HealthRouting(baseUrl, stateHolder, dependencyMonitor).configure(this)

  // Subscribe before adopting the already recorded exceptions. The other order would drop an
  // exception arriving between the two calls; this order counts it twice at worst.
  val uncaughtExceptionSubscription = GlobalExceptionCollector.onError { stateHolder.recordUncaughtException(it) }
  GlobalExceptionCollector.uncaughtExceptions?.let { stateHolder.recordUncaughtExceptions(it) }

  monitor.subscribe(ApplicationStopping) {
    uncaughtExceptionSubscription.dispose()
  }

  when (dependencyPolling) {
    DependencyPolling.OnDemand -> Unit
    DependencyPolling.Periodic -> if (probes.isNotEmpty()) {
      val dependencyPoll = dependencyMonitor.start(this)
      monitor.subscribe(ApplicationStopping) {
        dependencyPoll.dispose()
      }
    }
  }
}

/**
 * The [GlobalHealth] of this application, projected from its [ServiceStateHolder].
 *
 * Services that have not adopted [installHealthLayer] yet have no [ServiceStateHolder]. For those
 * the deprecated [GlobalHealth.fromUncaughtExceptions] is used.
 */
fun Application.globalHealth(): GlobalHealth {
  @Suppress("DEPRECATION")
  return findServiceStateHolder()?.let { GlobalHealth.from(it) } ?: GlobalHealth.fromUncaughtExceptions()
}

/**
 * Maps the framework-agnostic [HealthHttpStatus] onto a Ktor [HttpStatusCode].
 */
fun HealthHttpStatus.toHttpStatusCode(): HttpStatusCode = when (this) {
  HealthHttpStatus.Ok -> HttpStatusCode.OK
  HealthHttpStatus.ServiceUnavailable -> HttpStatusCode.ServiceUnavailable
}
