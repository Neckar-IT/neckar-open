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
 * Axis 3: Concrete instance/domain.
 *
 * The runtime identity of a service host: its hostname. Product-specific implementations add the
 * URLs / data their app needs.
 *
 * These types are published by the descriptors klib: consumers (including JS/browser code) declare
 * `api` on `internal/open/commons/descriptors`.
 *
 * @see ServiceHostRegistry for hostname-based lookup
 */
interface ServiceHost {
  /**
   * The hostname of the service.
   */
  val hostname: Hostname

  /**
   * The default service host for standalone commands with no product-specific host type:
   * a single host at [Hostname.localhost] that is also its own trivial registry. Used as the
   * `serviceHostRegistry` for local CLI tools and as the initial runtime-context host.
   */
  object Default : ServiceHost, FixedHostRegistry<Default> {
    override val hostname: Hostname
      get() = Hostname.localhost

    override val knownHosts: List<Default> = listOf(this)

    override fun resolveHost(hostname: Hostname?): Default {
      return this
    }
  }

  companion object {
    /**
     * Creates a [ServiceHost] identified solely by its [hostname], with no product-specific host
     * type. For a hostname that arrives as text at runtime — a secrets directory name, a
     * configured deploy target. A build script names a `Fleet` constant instead, which
     * `verifyFleet` enforces.
     */
    fun of(hostname: Hostname): ServiceHost = HostnameServiceHost(hostname)
  }
}

/**
 * Implementation backing [ServiceHost.of]: a [ServiceHost] defined by nothing but its [hostname].
 */
private data class HostnameServiceHost(
  override val hostname: Hostname,
) : ServiceHost {
  override fun toString(): String {
    return "ServiceHost(hostname='$hostname')"
  }
}
