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

import io.ktor.server.routing.Route
import it.neckar.runtime.context.DeploymentStage
import it.neckar.runtime.context.RuntimeContext

/**
 * Builds the routes of [build] only in the stage [DeploymentStage.Development]; in every other stage they do not
 * exist and answer 404. For routes that destroy data, such as clearing example data or deleting a tenant.
 *
 * The stage is read once, when the routes are built; a test passes a [stage] of its own.
 */
fun Route.developmentOnly(stage: DeploymentStage = initializedDeploymentStage(), build: Route.() -> Unit): Route {
  when (stage) {
    DeploymentStage.Development -> build()
    DeploymentStage.Staging, DeploymentStage.Demo, DeploymentStage.Production, DeploymentStage.QA -> Unit
  }
  return this
}

/**
 * The fallback before initialization is [DeploymentStage.Development]; reading it would open the routes in every
 * service that builds its routes before it reads its runtime context.
 */
private fun initializedDeploymentStage(): DeploymentStage {
  check(RuntimeContext.isInitialized()) { "developmentOnly needs the runtime context; initialize it before building the routes (a test calls RuntimeContext.initialize)" }
  return RuntimeContext.deploymentStage
}
