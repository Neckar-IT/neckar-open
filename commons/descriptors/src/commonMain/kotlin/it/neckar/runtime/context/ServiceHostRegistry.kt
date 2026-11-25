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
package it.neckar.runtime.context

/**
 * Resolves [ServiceHost]s by hostname. The registry type itself declares whether the service is
 * bound to a known host set ([FixedHostRegistry]) or runs at any host ([AnyHostRegistry]) — the
 * policy IS the type, so consumers dispatch on it (exhaustively) instead of reading a separate flag,
 * and the two can never disagree with the resolution behaviour.
 *
 * Usually implemented by the companion object of a [ServiceHost] enum / data class.
 */
sealed interface ServiceHostRegistry<out T : ServiceHost> {
  /**
   * Returns the host for [hostname], or a default when [hostname] is null/unknown
   * (the exact default is the implementing registry's choice).
   */
  fun resolveHost(hostname: Hostname?): T
}

/**
 * A service bound to a closed, compile-time-known set of [knownHosts] (e.g. Turboangebot,
 * Patterns). An unknown hostname is typically a configuration error. The resolution itself stays
 * with the implementation because the existing registries differ (some throw on unknown, some fall
 * back to localhost, some match additional fields).
 */
interface FixedHostRegistry<out T : ServiceHost> : ServiceHostRegistry<T> {
  /** The closed set of hosts this service is reachable at. */
  val knownHosts: List<T>
}

/**
 * A service that runs at any host (e.g. the redistributable Auto-Merge tool, which runs against any
 * GitLab on any host). An unknown hostname is resolved by [hostFor] from the runtime configuration
 * (`SERVICE_HOST`), not rejected.
 */
interface AnyHostRegistry<out T : ServiceHost> : ServiceHostRegistry<T> {
  /** Hosts with richer pre-defined data (e.g. the production constant); matched before [hostFor]. */
  val knownHosts: List<T>

  /** Returned when no hostname is configured (null). */
  val defaultHost: T

  /** Builds a host for an arbitrary [hostname] not in [knownHosts]. */
  fun hostFor(hostname: Hostname): T

  override fun resolveHost(hostname: Hostname?): T {
    if (hostname == null) return defaultHost
    return knownHosts.firstOrNull { it.hostname == hostname } ?: hostFor(hostname)
  }
}
