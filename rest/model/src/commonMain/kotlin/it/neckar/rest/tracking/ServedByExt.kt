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

/**
 * The header key identifying which backend service produced the response, e.g.
 * `PatternService`, `ElektroMeisterService`, `GitLabCompanionService`.
 *
 * Useful when multiple services sit behind the same reverse proxy and a response
 * looks wrong — the header pinpoints which one to inspect.
 *
 * Kept under the legacy `X-` prefix on purpose: this header is set in multiple layers
 * of the stack (Ktor, Nginx error pages, Traefik middleware) and is an established
 * de-facto cross-stack convention (Fastly, Akamai use the same name). Renaming to
 * `NeckarIT-Served-By` would force every reverse proxy config to follow — not worth
 * the churn for a header that is conceptually closer to `X-Forwarded-For` than to
 * the rest of the [it.neckar.rest.PlatformHeader] family.
 */
@Suppress("UnusedReceiverParameter")
val HttpHeaders.XServedBy: String
  get() {
    return "X-Served-By"
  }
