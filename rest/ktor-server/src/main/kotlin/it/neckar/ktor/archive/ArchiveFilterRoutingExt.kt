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
package it.neckar.ktor.archive

import io.ktor.http.Parameters
import it.neckar.open.http.UrlParameterName
import it.neckar.rest.InvalidQueryParameterException
import it.neckar.rest.archive.ArchiveFilter
import it.neckar.rest.openapi.dsl.OpenApiRequestBuilder

/**
 * Standard query parameter name for the archive filter on listing endpoints.
 *
 * Shared across every listing endpoint of every service — they all accept the same parameter
 * name. Values correspond to [ArchiveFilter] enum names (`OnlyActive`, `Archived`, `All`).
 */
const val ArchiveFilterQueryParamName: String = "archiveFilter"

/**
 * [UrlParameterName] wrapper around [ArchiveFilterQueryParamName] — used in OpenAPI
 * `.doc { request { parameterEnum<ArchiveFilter>(ArchiveFilterUrlParameter) } }` declarations.
 */
val ArchiveFilterUrlParameter: UrlParameterName = UrlParameterName(ArchiveFilterQueryParamName)

/**
 * Reads the optional archiveFilter query parameter. Missing or blank → [ArchiveFilter.OnlyActive].
 *
 * An unparseable value throws [InvalidQueryParameterException], which StatusPages maps to 400 Bad Request.
 * Silently defaulting would hide frontend bugs: the REST API is a BFF, so a wrong value is
 * always a caller error, not a user error.
 *
 * Lives in this shared module because the spec code generator emits routings that call it under
 * this package name — a copy per service would be the hand-written glue the generator exists to
 * avoid.
 */
fun Parameters.archiveFilterFromQuery(): ArchiveFilter {
  val raw = get(ArchiveFilterQueryParamName)?.trim()?.ifEmpty { null } ?: return ArchiveFilter.OnlyActive
  return ArchiveFilter.entries.find { it.name == raw }
    ?: throw InvalidQueryParameterException("Invalid $ArchiveFilterQueryParamName '$raw', expected one of ${ArchiveFilter.entries.joinToString { it.name }}")
}

/**
 * OpenAPI declaration for the archiveFilter query parameter, shared by every listing route.
 *
 * Use inside a route's `.doc { request { archiveFilterParameter() } }` block. Shared for the same
 * reason as [archiveFilterFromQuery]: the generator emits calls to it.
 */
fun OpenApiRequestBuilder.archiveFilterParameter() {
  parameterEnum<ArchiveFilter>(ArchiveFilterUrlParameter) {
    description = "Filter the listing by archive state. Defaults to OnlyActive — archived entries stay hidden unless the filter is switched."
    required = false
  }
}
