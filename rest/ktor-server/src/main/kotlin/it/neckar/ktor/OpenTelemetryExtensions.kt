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

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.queryString
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes
import io.opentelemetry.semconv.UrlAttributes
import it.neckar.ktor.tracking.MDCKey
import it.neckar.ktor.tracking.MDCKeys
import it.neckar.ktor.tracking.OtelIdentityPlugin
import it.neckar.ktor.tracking.XTraceIdHeaderPlugin
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.version.VersionInformation
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.rest.tracking.XTraceId

/**
 * The **single canonical entry point** for installing OpenTelemetry in any Ktor service
 * in this codebase. Installs a fixed bundle:
 *
 * 1. [KtorServerTelemetry] — server spans with W3C trace propagation and standard semantic
 *    conventions (method, path, status code), plus a baseline list of captured HTTP headers
 *    that never carry secrets ([HttpHeaders.UserAgent], [HttpHeaders.XRequestId] on the
 *    request; [HttpHeaders.ContentType], [HttpHeaders.XTraceId] on the response). Server
 *    spans are marked with [StatusCode.ERROR] on
 *    HTTP 4xx as well as 5xx — by default the upstream extractor only marks 5xx, which leaves
 *    4xx out of Tempo's "Error Traces" view. Additionally, MDC entries listed in
 *    [MdcKeysOnSpan] are copied to the span as `mdc.<key>` attributes for log↔trace correlation,
 *    and `url.query` keeps the names of [redactedQueryParameters] with the value `REDACTED`.
 * 2. [XTraceIdHeaderPlugin] — writes the OTel server-span trace ID into every response as
 *    `NeckarIT-Trace-Id` so frontends and humans can jump straight to the trace in Grafana/Tempo.
 * 3. [OtelIdentityPlugin] — enriches spans with `enduser.id` / `tenant.id` (from the JWT
 *    principal, if Authentication is installed) and `session.id` (from the `NeckarIT-Session-Id`
 *    request header). Safe to install unconditionally; missing JWT/session inputs are no-ops.
 *
 * Use [additionalCapturedRequestHeaders] / [additionalCapturedResponseHeaders] when a service
 * needs to attach extra headers to its spans (e.g. webhook services capturing `x-gitlab-event`).
 * Never put authentication, cookie, or other secret-bearing headers in either list.
 *
 * **Do not install [KtorServerTelemetry] directly anywhere else.** Doing so bypasses
 * [XTraceIdHeaderPlugin] and [OtelIdentityPlugin], which means the service silently drops
 * the trace-id response header and the identity span enrichment. A service that genuinely
 * cannot use OpenTelemetry must skip the plugin entirely and document the reason at the
 * call site.
 *
 * The SDK is configured by environment variables — see [AutoConfiguredOpenTelemetrySdk]:
 * - `OTEL_SERVICE_NAME` — service name for traces
 * - `OTEL_EXPORTER_OTLP_ENDPOINT` — OTLP endpoint (e.g., `http://tempo:4317`)
 * - `OTEL_EXPORTER_OTLP_PROTOCOL` — protocol (`grpc` or `http/protobuf`)
 * - `OTEL_TRACES_EXPORTER` — exporter type (`otlp`, `none`)
 * - `OTEL_METRICS_EXPORTER` — set to `none` if metrics are handled by Micrometer
 * - `OTEL_LOGS_EXPORTER` — set to `none` if logs are handled by loki4j
 *
 * The `service.version` resource attribute is added programmatically from [VersionInformation]
 * so traces are always tagged with the deployed git hash without manual Docker Compose wiring.
 *
 * MDC injection (`trace_id`/`span_id`) comes from the `logback-mdc-1.0` instrumentation of the
 * OpenTelemetry Java agent, see `OtelAgentDefaults`.
 *
 * When `OTEL_TRACES_EXPORTER` is unset (typical for local development without OTel collector),
 * the SDK falls back to a no-op implementation. [XTraceIdHeaderPlugin] then sees an invalid span
 * and simply omits the response header — zero runtime overhead.
 */
fun Application.installOpenTelemetry(
  openTelemetry: OpenTelemetry = buildAutoConfiguredOpenTelemetry(),
  additionalCapturedRequestHeaders: List<String> = emptyList(),
  additionalCapturedResponseHeaders: List<String> = emptyList(),
  redactedQueryParameters: Set<String> = OAuthCallbackQueryParameters,
) {
  install(KtorServerTelemetry) {
    setOpenTelemetry(openTelemetry)

    // Headers worth keeping on every span for triage. Listing only headers that never
    // carry secrets — never add anything from `HttpHeaders.Authorization`, `HttpHeaders.Cookie`,
    // `HttpHeaders.SetCookie`, or any `x-api-key`-style header. The OpenTelemetry library
    // case-normalizes header names internally and produces lowercase
    // `http.request.header.<name>` / `http.response.header.<name>` span attributes regardless
    // of input case.
    capturedRequestHeaders(*(BaselineCapturedRequestHeaders + additionalCapturedRequestHeaders).toTypedArray())
    capturedResponseHeaders(*(BaselineCapturedResponseHeaders + additionalCapturedResponseHeaders).toTypedArray())

    // Mark 4xx responses as ERROR in addition to the default 5xx-only behavior. This makes
    // Tempo's "Error Traces" filter and the Grafana dashboard's error-traces table surface
    // client-error responses too. 401/403/404 are intentionally included — a spike of those
    // is worth seeing as an error signal. If real-world false positives become noisy, narrow
    // the predicate.
    spanStatusExtractor { previousExtractor ->
      val httpStatus = response?.status()
      if (httpStatus != null && httpStatus >= HttpStatusCode.BadRequest) {
        spanStatusBuilder.setStatus(StatusCode.ERROR)
      } else {
        previousExtractor.extract(spanStatusBuilder, request, response, error)
      }
    }

    // Copy a curated set of MDC entries to span attributes for log↔trace correlation. The
    // allow-list ([MdcKeysOnSpan]) is explicit on purpose: identity (`enduser.id`, `tenant.id`,
    // `session.id`) and `feature.flags` are already attached by `OtelIdentityPlugin`, and the
    // OTel logback integration already injects `trace_id`/`span_id` into MDC — none of those
    // need to be duplicated under an `mdc.*` prefix. Adding new MDC keys here must be a
    // deliberate decision so span size stays bounded and no PII slips through.
    //
    // Read MDC in `onEnd` (after Ktor plugins have populated it during call processing); at
    // `onStart` only thread-local entries inherited from outside the request are visible.
    attributesExtractor {
      onStart {
        val query = request.queryString()
        val redacted = redactQueryParameters(query, redactedQueryParameters)
        if (redacted != query) {
          attributes.put(UrlAttributes.URL_QUERY, redacted)
        }
      }
      onEnd {
        MdcKeysOnSpan.forEach { mdcKey ->
          mdcKey.get()?.let { value ->
            attributes.put(AttributeKey.stringKey("mdc.${mdcKey.value}"), value)
          }
        }
      }
    }
  }

  install(XTraceIdHeaderPlugin)
  install(OtelIdentityPlugin)
}

/**
 * Build an auto-configured OpenTelemetry SDK with programmatic resource attributes
 * for the service version ([VersionInformation.artifactIdentityShort]).
 *
 * These are merged with any attributes from `OTEL_RESOURCE_ATTRIBUTES` environment variable.
 * Programmatic attributes have lower priority — environment variables win if both set the same key.
 */
private fun buildAutoConfiguredOpenTelemetry(): OpenTelemetry {
  return AutoConfiguredOpenTelemetrySdk.builder()
    .addResourceCustomizer { existingResource, _ ->
      val versionResource = Resource.builder()
        .put(ServiceAttributes.SERVICE_VERSION, VersionInformation.artifactIdentityShort)
        .build()
      // existing (from env vars) takes precedence over programmatic
      versionResource.merge(existingResource)
    }
    .build()
    .openTelemetrySdk
}

/** The parameters of an OAuth callback, whose values stay out of traces. */
val OAuthCallbackQueryParameters: Set<String> = setOf("code", "state", "session_state")

/** [query] with the value of every parameter named in [names] replaced by `REDACTED`; a name is compared undecoded. */
internal fun redactQueryParameters(query: String, names: Set<String>): String {
  return query.split('&').joinToString("&") { parameter ->
    val name = parameter.substringBefore('=')
    if (name in names && '=' in parameter) "$name=REDACTED" else parameter
  }
}

private val BaselineCapturedRequestHeaders: List<String> = listOf(
  HttpHeaders.UserAgent,
  HttpHeaders.XRequestId,
)
private val BaselineCapturedResponseHeaders: List<String> = listOf(
  HttpHeaders.ContentType,
  HttpHeaders.XTraceId,
)

/**
 * MDC keys whose values are copied to the server span as `mdc.<key>` attributes.
 *
 * Allow-list, not full-MDC copy: identity (`enduser.id`/`tenant.id`/`session.id`) and feature
 * flags are already attached by [OtelIdentityPlugin]. `client.id` / `client.role` are attached
 * directly by `installCallLogging`. Only MDC keys with diagnostic value belong here.
 *
 * [MDCKeys.TraceId] and [MDCKeys.SpanId] are intrinsic to the span — but they are still copied
 * across as `mdc.trace_id` / `mdc.span_id` so that every server span carries an obvious smoke
 * test that the MDC bridge actually fires.
 */
private val MdcKeysOnSpan: List<MDCKey> = listOf(
  MDCKeys.CallId,
  MDCKeys.TraceId,
  MDCKeys.SpanId,
)

/**
 * Resolves the [OpenTelemetry] instance a service should hand to [installOpenTelemetry],
 * picking the right source depending on whether the OpenTelemetry Java Agent is attached.
 *
 * - When the agent is on the classpath (Docker image with the `otelAgent` convention plugin
 *   applied), it has already registered the SDK via [GlobalOpenTelemetry] before `main()` ran.
 *   Calling [AutoConfiguredOpenTelemetrySdk.initialize] again would create a second, conflicting
 *   SDK instance, so the agent's [GlobalOpenTelemetry] is reused.
 * - Otherwise (IDE, integration tests, the non-agent `agent-orchestrator`), a fresh SDK is built
 *   from the environment. Enablement is endpoint-driven (ADR 0170): the `EndpointGatedSdkEnablement`
 *   customizer (on the classpath via `ktor-server`) disables it when no OTLP endpoint is configured.
 *
 * Sets `otel.service.name` from [serviceDescriptor] before SDK construction so non-agent runs
 * still get the right service label — but only when neither the system property nor the
 * `OTEL_SERVICE_NAME` env variable is already set: OTel precedence is SysProp > Env, so an
 * unconditional setProperty would silently defeat every operator/developer override.
 *
 * This is the **single canonical resolver** for service entry points — do not hand-roll the
 * agent-detection dance in individual `*Command` classes.
 */
fun resolveServiceOpenTelemetry(serviceDescriptor: ServiceDescriptor): OpenTelemetry {
  if (System.getProperty("otel.service.name") == null && System.getenv("OTEL_SERVICE_NAME") == null) {
    System.setProperty("otel.service.name", serviceDescriptor.serviceName.kebabCaseName())
  }

  val agentActive: Boolean = runCatching {
    Class.forName("io.opentelemetry.javaagent.OpenTelemetryAgent")
  }.isSuccess

  return if (agentActive) {
    logger.info("OpenTelemetry Java Agent detected — using GlobalOpenTelemetry")
    GlobalOpenTelemetry.get()
  } else {
    logger.info("No Java Agent — initializing OpenTelemetry SDK from environment")
    AutoConfiguredOpenTelemetrySdk.initialize().openTelemetrySdk
  }
}

private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.OpenTelemetryExtensions")
