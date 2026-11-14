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
package it.neckar.ktor.tracking

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import it.neckar.rest.tracking.XTraceId

/**
 * Ktor plugin that writes the OpenTelemetry trace ID of the current server span into
 * every response as the [HttpHeaders.XTraceId] header.
 *
 * Set on **every** response — success and error, structured and unstructured. Frontends
 * use the value to build Grafana/Tempo deep-links and to render a copy-paste-friendly
 * trace ID on the error page.
 *
 * When no exporter is configured (local dev without OTEL_TRACES_EXPORTER) or the OTel SDK
 * is otherwise inactive, [currentTraceId] answers `null` and the header is not written.
 *
 * Installed automatically by `installOpenTelemetry()` so every service that wires up OTel
 * gets the header without extra configuration.
 */
val XTraceIdHeaderPlugin: ApplicationPlugin<Unit> = createApplicationPlugin("XTraceIdHeader") {
  onCallRespond { call ->
    currentTraceId()?.let { traceId ->
      call.response.headers.append(HttpHeaders.XTraceId, traceId.value)
    }
  }
}
