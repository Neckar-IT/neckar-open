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

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents a hostname
 */
@JvmInline
@Serializable
value class Hostname(val value: String) {
  init {
    require(value.isNotBlank()) { "Hostname must not be blank" }
    require(value.contains('@').not()) { "Hostname [$value] must not contain '@'" }
  }

  /**
   * The label before the first dot: `hafen-host.neckar.it` → `hafen-host`. What a deploy resource
   * names a host by when the domain is implied — a container name, a directory, a compose service.
   */
  val shortName: String
    get() = value.substringBefore('.')

  /**
   * The name a constant takes for this host: `worker-01-host` → `Worker01Host`, `dev.elektromeister.neckar.it` → `DevElektromeister`.
   * The `Host` part stays: `auth.neckar.it` is Keycloak, `auth-host.neckar.it` the machine under it.
   */
  val constantName: String
    get() = upperCamelCase(value.removeSuffix(".neckar.it"), '-', '.')

  /**
   * Whether the name resolves to the machine it is read on: `localhost` and its subdomains (RFC 6761), and
   * `localdev` and its subdomains, which `/etc/hosts` maps to the loopback address of the local dev setup.
   */
  val namesLocalMachine: Boolean
    get() = LocalMachineNames.any { name -> value.equals(name, ignoreCase = true) || value.endsWith(".$name", ignoreCase = true) }

  override fun toString(): String {
    return value
  }

  companion object {
    /**
     * Represents "localhost"
     */
    private const val LocalhostName = "localhost"

    val localhost: Hostname = Hostname(LocalhostName)

    /**
     * The names that resolve to the local machine, each with its subdomains.
     */
    private val LocalMachineNames: List<String> = listOf(LocalhostName, "localdev")

    fun nullable(hostnameAsString: String?): Hostname? {
      if (hostnameAsString == null) {
        return null
      }
      val trimmed = hostnameAsString.trim()
      if (trimmed.isEmpty()) {
        return null
      }
      return Hostname(trimmed)
    }
  }
}
