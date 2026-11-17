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

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider
import io.opentelemetry.sdk.resources.Resource

/**
 * Derives OpenTelemetry resource attributes from the runtime-context environment — the same
 * `DEPLOYMENT_STAGE` / `EXECUTION_ENVIRONMENT` / `SERVICE_HOST` values the application reads into
 * `it.neckar.runtime.context.RuntimeContext` at bootstrap.
 *
 * Discovered via `META-INF/services`. Under the deployed OpenTelemetry Java Agent this class must be
 * loaded as an **agent extension** (`-Dotel.javaagent.extensions=…`): the agent does **not** pick up
 * `ResourceProvider`s from the application classpath (verified against agent 2.29.0). The plain
 * autoconfigured SDK used by non-agent runs (IDE / integration tests) *does* load application-classpath
 * providers, so the same class serves both paths.
 *
 * The env key names are inlined (not referenced from `RuntimeContextEnv`) so this class carries no
 * first-party dependency: the agent loads the extension in an isolated classloader that only has the
 * agent's own OpenTelemetry SDK, not the application classpath.
 *
 * Attributes (set only when the underlying value is present):
 * - `deployment.environment` — OTel-standard stage label, lowercased (`production` / `development` / `demo`).
 * - `service.namespace` — constant `neckar-it`.
 * - `neckar.execution_environment` — technical execution environment (`Docker`, `CI`, …).
 * - `neckar.service_host` — the service's public host (DNS).
 * - `service.version` — the short image digest.
 */
class NeckarRuntimeContextResourceProvider : ResourceProvider {
  override fun createResource(config: ConfigProperties): Resource {
    val builder = Resource.builder()

    builder.put(NeckarResourceAttributes.ServiceNamespace, ServiceNamespaceValue)

    resolve(DeploymentStageEnvKey)?.let { builder.put(NeckarResourceAttributes.DeploymentEnvironment, it.lowercase()) }
    resolve(ExecutionEnvironmentEnvKey)?.let { builder.put(NeckarResourceAttributes.ExecutionEnvironment, it) }
    resolve(ServiceHostEnvKey)?.let { builder.put(NeckarResourceAttributes.ServiceHost, it) }
    resolve(ImageDigestEnvKey)?.let(::shortImageDigestOrNull)?.let { builder.put(NeckarResourceAttributes.ServiceVersion, it) }

    return builder.build()
  }

  /** Resolves a value from a JVM system property first, then the process environment. */
  private fun resolve(key: String): String? {
    return (System.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }
  }

  /**
   * The 12 hex characters behind `sha256:`; null for the `unpinned` marker of a branch deploy and any malformed value, which the
   * service itself fails on at start — the agent does not fail the JVM ahead of it.
   */
  private fun shortImageDigestOrNull(imageDigest: String): String? {
    return imageDigest.takeIf { WellFormedImageDigest.matches(it) }?.substring(ImageDigestAlgorithm.length, ImageDigestAlgorithm.length + ShortImageDigestLength)
  }

  companion object {
    // Env keys — mirror it.neckar.runtime.context.RuntimeContextEnv (inlined to keep this JAR dependency-free).
    private const val DeploymentStageEnvKey: String = "DEPLOYMENT_STAGE"
    private const val ExecutionEnvironmentEnvKey: String = "EXECUTION_ENVIRONMENT"
    private const val ServiceHostEnvKey: String = "SERVICE_HOST"
    // Mirror VersionInformation.ImageDigestEnvVar, ImageDigestText.Algorithm and VersionInformation.ShortHashLength.
    private const val ImageDigestEnvKey: String = "APP_IMAGE_DIGEST"
    private const val ImageDigestAlgorithm: String = "sha256:"
    private const val ShortImageDigestLength: Int = 12
    private val WellFormedImageDigest: Regex = Regex("^" + Regex.escape(ImageDigestAlgorithm) + "[a-f0-9]{64}$")

    private const val ServiceNamespaceValue: String = "neckar-it"
  }
}
