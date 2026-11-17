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
package it.neckar.rest.meta

import it.neckar.open.annotations.DataView
import it.neckar.rest.error.GlobalHealth
import kotlinx.serialization.Serializable

/**
 * Identity, build provenance, runtime classification and health of a running service.
 *
 * Aggregates four sources rather than projecting one, so there is no conversion extension: the
 * service descriptor, [it.neckar.open.version.VersionInformation], the process-wide
 * [it.neckar.runtime.context.RuntimeContext] and the application's current health.
 */
@DataView("Service profile: what a service reports about itself when asked who and where it is")
@Serializable
data class ServiceInfo(
  /**
   * Human-readable name of the service
   */
  val serviceName: String,

  /**
   * Current version of the service
   */
  val version: String,

  /**
   * The build the running artifact was cut from.
   */
  val versionInfo: MetaTags,

  /**
   * How the running process classified its own deployment.
   */
  val runtimeContext: RuntimeContextDetails,

  /**
   * Current health status of the service
   */
  val health: GlobalHealth,
)
