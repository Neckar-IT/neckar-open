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
package it.neckar.open.health

import it.neckar.open.tracing.TraceId

/**
 * The lifecycle state of a service, held in-process by the [ServiceStateHolder].
 *
 * This is the single source of truth; the health probes only project it. Transitions follow
 * `Starting → Verifying → (Migrating) → Ready ↔ Degraded`, with `Fatal` reachable from any
 * non-fatal state and terminal.
 *
 * The wire-facing `dependency-unavailable` and `schema-incompatible` values are not lifecycle
 * states: the first is derived from a down required dependency, the second from [Fatal] with
 * [FatalReason.SchemaIncompatible].
 */
sealed interface ServiceState {
  /**
   * The HTTP server is binding; nothing is reachable yet.
   */
  data object Starting : ServiceState

  /**
   * The server is up and verifying its dependencies and schema before accepting business traffic.
   */
  data object Verifying : ServiceState

  /**
   * A schema migration is in progress — this instance runs it, or waits for the instance that does.
   */
  data object Migrating : ServiceState

  /**
   * Fully operational.
   */
  data object Ready : ServiceState

  /**
   * Operational, but an optional dependency is down.
   */
  data object Degraded : ServiceState

  /**
   * A permanent failure. Liveness deliberately stays up so no health-check restart loops on it.
   */
  data class Fatal(
    /**
     * Why the service is fatal — drives the wire `state`.
     */
    val reason: FatalReason,
    /**
     * Human-readable detail for logs and the alert. Never answered to a caller: it carries whatever
     * the failing code put into its exception message, and the health endpoints are unauthenticated.
     */
    val message: String,
    /**
     * The fully qualified type of the exception behind the failure (e.g.
     * `it.neckar.open.mongodb.migration.MigrationBlockedException`), or `null` when no exception
     * caused it. Answered to a caller — a type name is a classification, not a text we chose for it.
     */
    val errorType: String? = null,
    /**
     * The trace of the error span recorded when this state was entered, so a caller of the health
     * endpoints can open message and stack trace in Grafana/Tempo. `null` without a configured
     * exporter.
     */
    val traceId: TraceId? = null,
  ) : ServiceState

  /**
   * Whether a transition from this state to [target] is allowed.
   */
  fun canTransitionTo(target: ServiceState): Boolean {
    if (this == target) return true
    if (target is Fatal) return this !is Fatal
    return when (this) {
      Starting -> target == Verifying
      Verifying -> target == Migrating || target == Ready
      Migrating -> target == Ready
      Ready -> target == Degraded
      Degraded -> target == Ready
      is Fatal -> false
    }
  }
}

/**
 * The reason a service is [ServiceState.Fatal].
 */
enum class FatalReason {
  /**
   * The connected database schema is incompatible (wrong type, too new, no migration path, or a
   * migrator's dry run rejected the data).
   */
  SchemaIncompatible,

  /**
   * A migration failed, or an earlier failed one blocks this start. The data is migrated in part and
   * needs a human — its own reason so an alert can match on it rather than on a message text.
   */
  MigrationFailed,

  /**
   * Any other permanent failure (uncaught error, broken configuration).
   */
  Unspecified,
}
