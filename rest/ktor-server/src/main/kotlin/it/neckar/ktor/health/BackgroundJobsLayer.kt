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

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.ServerReady
import io.opentelemetry.api.OpenTelemetry
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.health.BackgroundJobRegistry
import it.neckar.open.health.recordBackgroundJobsToOpenTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.health.BackgroundJobsLayer")

/**
 * Connects the background jobs of this service to its lifecycle — the one call every service with a
 * polling or scheduler job makes in `Application.configure()`:
 *
 * - **Publishes** the [BackgroundJobRegistry] as the gauges `background_job.stale` and
 *   `background_job.seconds_since_last_cycle`. Without them a service records its cycles and reports
 *   them in `GET /health`, but the Grafana rule "Background Job Stale" never sees it.
 * - **Cancels** [backgroundScope] when the application stops. Without that the jobs outlive the
 *   service that owns them and keep working against GitLab, Mattermost or the database.
 * - **Watches** the jobs with an [EssentialJobWatchdog]. A job that stopped cycling ends the process
 *   when the process start armed the server ([StallReactionAttribute]); any other application —
 *   every `testApplication` — only logs it.
 *
 * All in one call on purpose: they are parts of the same decision ("this service runs background
 * jobs"), and a service that makes one call has all of them.
 *
 * Call order does not matter — the gauges read the registry at collection time, so jobs registered
 * afterwards are picked up on their own.
 *
 * @param openTelemetry the instance the service resolved itself (`resolveServiceOpenTelemetry`).
 * @param backgroundScope the single scope every background job of this service runs in. `null` when
 * no job runs in a scope of the service's own, as the JWKS refresh runs in the application scope.
 */
fun Application.installBackgroundJobs(
  openTelemetry: OpenTelemetry,
  backgroundScope: CoroutineScope? = null,
) {
  val registration = BackgroundJobRegistry.recordBackgroundJobsToOpenTelemetry(openTelemetry)

  val watchdog = EssentialJobWatchdog(attributes.getOrNull(StallReactionAttribute) ?: StallReaction(WatchdogMode.LogOnly))
  //After the bind: a server that fails to bind never stops, and testApplication never binds
  monitor.subscribe(ServerReady) { watchdog.start() }

  //Before ApplicationStopping cancels the jobs: their end is the shutdown, not a stall
  monitor.subscribe(ApplicationStopPreparing) {
    watchdog.stop()
  }

  monitor.subscribe(ApplicationStopping) {
    //For a caller that raises ApplicationStopping alone
    watchdog.stop()
    registration.dispose()

    backgroundScope?.let {
      logger.info("Cancelling the background jobs")
      it.cancel("The service is stopping")
    }
  }
}
