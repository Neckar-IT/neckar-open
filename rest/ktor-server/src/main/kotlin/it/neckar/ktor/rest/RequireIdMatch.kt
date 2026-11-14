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
package it.neckar.ktor.rest

import io.ktor.server.plugins.BadRequestException

/**
 * Asserts that the entity ID inside a request body matches the ID extracted from
 * the URL path. A mismatch is a structurally invalid request (the client routed
 * a body for one entity to the URL of another), not a domain outcome — therefore
 * throws [BadRequestException], which the global StatusPages plugin maps to
 * HTTP 400 Bad Request.
 *
 * One implementation for every upsert endpoint, so the error message is identical
 * everywhere and one fix benefits all. Lives in this shared module because the spec
 * code generator emits routings that call it — a copy per service would be the
 * hand-written glue the generator exists to avoid.
 *
 * Usage:
 * ```kotlin
 * val bookId = call.parameters.bookIdFromPath()
 * val request = call.receive<UpsertBookRequest>()
 * requireIdMatchesPath(entityName = "Book", bodyId = request.id, pathId = bookId)
 * ```
 */
fun requireIdMatchesPath(entityName: String, bodyId: Any, pathId: Any) {
  if (bodyId != pathId) {
    throw BadRequestException("$entityName ID in request body ($bodyId) does not match the ID from the URL path ($pathId)")
  }
}
