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
package it.neckar.rest.tracking

import io.ktor.http.*
import it.neckar.rest.PlatformHeader

/**
 * The header key for the OpenTelemetry trace ID of the server span that processed
 * the request.
 *
 * Set on every response (success and error) by the `XTraceIdHeaderPlugin`. Frontends
 * read it to render Grafana/Tempo deep-links; humans copy it into Grafana Explore to
 * look up the corresponding logs and trace.
 *
 * Value is the 32-character lowercase hex W3C Trace-Context format, e.g.
 * `4bf92f3577b34da6a3ce929d0e0e4736`.
 *
 * Delegates to [PlatformHeader.TraceId], the single source of truth shared with the TypeScript frontend.
 */
@Suppress("UnusedReceiverParameter")
val HttpHeaders.XTraceId: String
  get() {
    return PlatformHeader.TraceId.headerName
  }
