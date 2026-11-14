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
import it.neckar.runtime.context.DeploymentStage
import it.neckar.runtime.context.ExecutionEnvironment
import it.neckar.runtime.context.Hostname
import it.neckar.runtime.context.RuntimeContext
import kotlinx.serialization.Serializable

/**
 * The wire projection of [RuntimeContext] — what the running process resolved from its environment.
 *
 * Two fields of the runtime context are deliberately absent: `host` is a product-specific
 * [it.neckar.runtime.context.ServiceHost] implementation carrying deploy URLs that belong to no
 * client, so only its [Hostname] survives the projection. `initialValue` marks whether bootstrap has
 * run and means nothing outside the process.
 */
@DataView("Diagnostics: shows how a running service classified its own deployment")
@Serializable
data class RuntimeContextDetails(
  /**
   * Business / lifecycle environment — which functional stage of delivery the service is in.
   */
  val deploymentStage: DeploymentStage,

  /**
   * Technical execution environment — where/how the service process is running.
   */
  val executionEnvironment: ExecutionEnvironment,

  /**
   * The public hostname the service is deployed on.
   */
  val hostname: Hostname,

  /**
   * Whether the process runs with a debugger attached — relaxed timeouts, verbose logging.
   */
  val debugMode: Boolean,

  /**
   * Whether the process runs inside a unit test — integrations replaced by mocks.
   */
  val inUnitTest: Boolean,
)

/**
 * Projects the process-wide runtime context onto its wire representation.
 */
fun RuntimeContext<*>.toDetails(): RuntimeContextDetails {
  return RuntimeContextDetails(
    deploymentStage = stage,
    executionEnvironment = executionEnvironment,
    hostname = host.hostname,
    debugMode = debugMode,
    inUnitTest = inUnitTest,
  )
}
