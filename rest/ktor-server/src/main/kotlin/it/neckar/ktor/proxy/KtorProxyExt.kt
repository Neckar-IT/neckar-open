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
package it.neckar.ktor.proxy

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.forwardedheaders.*
import it.neckar.open.http.Url
import it.neckar.projects.common.Port


/**
 * Installs support for reverse proxies (e.g., nginx, Apache, HAProxy, ...)
 *
 * WARNING: for security, do not include this if not behind a reverse proxy
 */
fun Application.installReverseProxySupport() {
  install(ForwardedHeaders) // WARNING: for security, do not include this if not behind a reverse proxy
  install(XForwardedHeaders) // WARNING: for security, do not include this if not behind a reverse proxy
}

/**
 * Creates a [Url.Absolute] from this [RequestConnectionPoint].
 * Standard ports (80 for HTTP, 443 for HTTPS) are omitted for cleaner output.
 */
fun RequestConnectionPoint.toUrl(): Url.Absolute {
  return Url.from(scheme = scheme, host = serverHost, port = Port(serverPort))
}
