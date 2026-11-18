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

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.*
import it.neckar.common.featureflags.FeatureFlag
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.common.featureflags.FeatureFlagsElement
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.projects.common.DefaultFeatureFlagsEnvironmentVariable
import kotlinx.coroutines.*

/**
 * Installs the [FeatureFlagsPlugin] with [defaultFlags], else [defaultFeatureFlagsFromEnvironment], unless already installed.
 * Safe to call multiple times (e.g., in both service setup and test utilities).
 */
fun Application.installFeatureFlagSupport(defaultFlags: FeatureFlags? = null) {
  if (pluginOrNull(FeatureFlagsPlugin) == null) {
    install(FeatureFlagsPlugin) {
      this.defaultFlags = defaultFlags ?: defaultFeatureFlagsFromEnvironment()
    }
  }
}

/**
 * Throws on an unknown key, so a typo in the Gradle property `localDevFeatureFlags` stops the service start.
 */
internal fun defaultFeatureFlagsFromEnvironment(): FeatureFlags {
  return parseDefaultFeatureFlags(System.getenv(DefaultFeatureFlagsEnvironmentVariable.value))
}

/**
 * Decodes [value] of [DefaultFeatureFlagsEnvironmentVariable], throws on a key outside [FeatureFlag.available].
 */
internal fun parseDefaultFeatureFlags(value: String?): FeatureFlags {
  val flags = FeatureFlags.decodeFromString(value)
  val unknownKeys = flags.flags.filter { flag -> flag !in FeatureFlag.available }.map { flag -> flag.key.value }
  require(unknownKeys.isEmpty()) {
    "Unknown feature flags in $DefaultFeatureFlagsEnvironmentVariable: $unknownKeys; known: ${FeatureFlag.available.joinToString { flag -> flag.key.value }}"
  }
  return flags
}

/**
 * The [FeatureFlagsPluginConfiguration.defaultFlags] of the installed [FeatureFlagsPlugin]; [extractFeatureFlags] adds them to every
 * request.
 */
val DefaultFeatureFlagsAttribute: AttributeKey<FeatureFlags> = AttributeKey("DefaultFeatureFlags")

class FeatureFlagsPluginConfiguration {
  /**
   * Added to the flags of every request.
   */
  var defaultFlags: FeatureFlags = FeatureFlags.empty
}

/**
 * Ktor plugin that extracts feature flags from every incoming request (headers + query params),
 * adds [FeatureFlagsPluginConfiguration.defaultFlags] and installs them in the coroutine context.
 *
 * After installation, any suspend function in the request pipeline can read the flags
 * via [it.neckar.common.featureflags.currentFeatureFlags] — no explicit parameter passing required.
 *
 * Simulation effects:
 * - [FeatureFlag.realisticLatency] delays every call, also a 404 and a CORS preflight.
 * - [FeatureFlag.slowServer] delays and [FeatureFlag.forceServerError] fails every call that resolves to a route without
 *   [exemptFromServerSimulation]; health, metrics, meta, OpenAPI and MongoDB meta routes carry the marker.
 *
 * Install once at the application level via [installFeatureFlagSupport]; the plugin installs [RoutingRoot] if it is missing.
 */
val FeatureFlagsPlugin: ApplicationPlugin<FeatureFlagsPluginConfiguration> = createApplicationPlugin("FeatureFlags", ::FeatureFlagsPluginConfiguration) {
  application.attributes.put(DefaultFeatureFlagsAttribute, pluginConfig.defaultFlags)
  application.intercept(ApplicationCallPipeline.Plugins) {
    val flags = call.request.extractFeatureFlags()
    withContext(FeatureFlagsElement(flags)) {
      call.appendSimulatedDelay(flags.delayIfRealisticLatencyEnabled())
      proceed()
    }
  }
  application.routing {
    install(ServerSimulationPlugin)
  }
}

private val ServerSimulationExemptAttribute: AttributeKey<Unit> = AttributeKey("ServerSimulationExempt")

/**
 * Keeps [FeatureFlag.slowServer] and [FeatureFlag.forceServerError] away from exactly this route; reserved for diagnosis,
 * among them the DevTools reading the available feature flags to switch them off again. Routes below it keep simulating.
 */
@IgnorableReturnValue
fun Route.exemptFromServerSimulation(): Route {
  attributes.put(ServerSimulationExemptAttribute, Unit)
  return this
}

/**
 * Runs [handler] in the [ApplicationCallPipeline.Call] phase of the route: after every route-scoped plugin of the resolved route,
 * [io.ktor.server.auth.authenticate] included, before the route handler.
 */
private object BeforeRouteHandler : Hook<suspend (ApplicationCall) -> Unit> {
  override fun install(pipeline: ApplicationCallPipeline, handler: suspend (ApplicationCall) -> Unit) {
    pipeline.intercept(ApplicationCallPipeline.Call) {
      handler(call)
    }
  }
}

/**
 * Runs on the routing root, because only there the resolved route and its [exemptFromServerSimulation] marker are known.
 * The effects run after authentication: a request without valid credentials answers 401 under [FeatureFlag.forceServerError].
 */
private val ServerSimulationPlugin: RouteScopedPlugin<Unit> = createRouteScopedPlugin("ServerSimulation") {
  on(BeforeRouteHandler) { call ->
    val resolvedRoute: Route = (call as RoutingPipelineCall).route
    if (resolvedRoute.attributes.contains(ServerSimulationExemptAttribute)) {
      return@on
    }
    val flags: FeatureFlags = currentCoroutineContext()[FeatureFlagsElement].requireNotNull { "FeatureFlagsPlugin sets the FeatureFlagsElement" }.flags
    call.appendSimulatedDelay(flags.delayIfSlowServerEnabled())
    flags.forceInternalErrorIfEnabled()
  }
}

/**
 * Returns the [FeatureFlags] from the coroutine context.
 * Throws if [FeatureFlagsPlugin] is not installed — use [installFeatureFlagSupport] in `Application.configure()`.
 *
 * Use this in route handlers. For general suspend functions (business logic, tests),
 * use [it.neckar.common.featureflags.currentFeatureFlags] which returns [FeatureFlags.empty] as fallback.
 */
suspend fun RoutingContext.featureFlags(): FeatureFlags {
  return currentCoroutineContext()[FeatureFlagsElement]?.flags
    ?: error("FeatureFlagsPlugin not installed. Call installFeatureFlagSupport() in Application.configure().")
}
