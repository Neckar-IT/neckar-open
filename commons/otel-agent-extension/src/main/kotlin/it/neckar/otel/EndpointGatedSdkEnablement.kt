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
package it.neckar.otel

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider

/**
 * Gates the OpenTelemetry SDK on the presence of an OTLP endpoint — the endpoint is the switch. When
 * none is configured, and the operator has not set `otel.sdk.disabled` explicitly, this injects
 * `otel.sdk.disabled=true` so an unconfigured run (bare `docker run`, IDE, test) does not retry
 * exports against the `localhost:4317` default. An explicit `OTEL_SDK_DISABLED` (either value) wins.
 *
 * The positive activation model of ADR 0170 (supersedes ADR 0155's two-variable form). It runs
 * in-JVM via OpenTelemetry's own SPI — not an entrypoint wrapper — so it stays Jib-compatible.
 * Loaded from this single source both ways, like [NeckarRuntimeContextResourceProvider]: as an agent
 * extension for deployed services, and via `ktor-server`'s `api(otel-agent-extension)` classpath
 * dependency for non-agent runs (`agent-orchestrator`, IDE, tests).
 */
class EndpointGatedSdkEnablement : AutoConfigurationCustomizerProvider {
  override fun customize(autoConfiguration: AutoConfigurationCustomizer) {
    autoConfiguration.addPropertiesCustomizer { config ->
      enablementOverrides(
        endpointConfigured = anyEndpointConfigured(EndpointProperties.map { config.getString(it) }),
        sdkDisabledExplicitlySet = System.getProperty(sdkDisabledProperty) != null || System.getenv(sdkDisabledEnv) != null,
      )
    }
  }

  companion object {
    private const val sdkDisabledProperty: String = "otel.sdk.disabled"
    private const val sdkDisabledEnv: String = "OTEL_SDK_DISABLED"

    /** General OTLP endpoint plus the three per-signal endpoints — any one of them means "on". */
    private val EndpointProperties: List<String> = listOf(
      "otel.exporter.otlp.endpoint",
      "otel.exporter.otlp.traces.endpoint",
      "otel.exporter.otlp.metrics.endpoint",
      "otel.exporter.otlp.logs.endpoint",
    )

    /** True when at least one of the resolved endpoint values is non-blank. */
    internal fun anyEndpointConfigured(endpointValues: List<String?>): Boolean {
      return endpointValues.any { it?.isNotBlank() == true }
    }

    /**
     * The property overrides to apply: force the SDK off only when no endpoint is configured **and**
     * the operator has not set `otel.sdk.disabled` themselves. Pure function for testability.
     */
    internal fun enablementOverrides(endpointConfigured: Boolean, sdkDisabledExplicitlySet: Boolean): Map<String, String> {
      if (endpointConfigured || sdkDisabledExplicitlySet) {
        return emptyMap()
      }
      return mapOf(sdkDisabledProperty to "true")
    }
  }
}
