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

import io.ktor.http.Parameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import it.neckar.ktor.archive.ArchiveResult
import it.neckar.ktor.archive.UnarchiveResult
import it.neckar.ktor.archive.createFor
import it.neckar.ktor.authorization.authorize
import it.neckar.ktor.featureFlags
import it.neckar.ktor.handle
import it.neckar.ktor.route
import it.neckar.ktor.server.openapi.ktor.doc
import it.neckar.open.authorization.Capability
import it.neckar.open.authorization.CoreCapability
import it.neckar.open.http.UrlPattern
import it.neckar.open.tenancy.Tenant
import it.neckar.rest.ArchiveResponse
import it.neckar.rest.ArchiveResponseExample
import it.neckar.rest.FoundResponse
import it.neckar.rest.FoundResponseExample
import it.neckar.rest.UnarchiveResponse
import it.neckar.rest.UnarchiveResponseExample

/**
 * The words the OpenAPI texts of a standard entity route use for the entity: "Archive [article] [singular]".
 */
data class EntityRouteLabel(
  /**
   * Lower case, e.g. `book edition type`.
   */
  val singular: String,
  /**
   * `a` or `an`.
   */
  val article: String,
)

/**
 * `POST [pattern]`: archives the entity and answers with an [ArchiveResponse] of the referencing entity types [E].
 * The OpenAPI description names [unarchivePattern] and, when [E] has constants, the referencing records.
 * [capability] guards the route, `CoreCapability.WriteData` unless the project names its own key.
 */
inline fun <reified E : Enum<E>, Id> Route.archiveEntityRoute(
  pattern: UrlPattern.Relative1,
  unarchivePattern: UrlPattern.Relative1,
  label: EntityRouteLabel,
  noinline tenantId: suspend RoutingContext.() -> Tenant.Id,
  noinline idFromPath: Parameters.() -> Id,
  noinline archive: suspend (Id, Tenant.Id) -> ArchiveResult<E>,
  descriptionNote: String? = null,
  capability: Capability = CoreCapability.WriteData,
) {
  val archiveDescription = archiveEntityDescription(unarchivePattern, label, referencedBy = enumValues<E>().isNotEmpty(), descriptionNote, capability)

  route(pattern) {
    authorize(capability) {
      post {
        featureFlags().handle()

        val callerTenantId = tenantId()
        val result = archive(call.parameters.idFromPath(), callerTenantId)

        call.respond<ArchiveResponse<E>>(ArchiveResponse.createFor(result))
      }.doc {
        summary = "Archive ${label.article} ${label.singular}"
        description = archiveDescription

        noRequestBody()

        responding<ArchiveResponse<E>>(
          successExample = ArchiveResponseExample.archived,
          failureExamples = listOf(ArchiveResponseExample.alreadyArchived, ArchiveResponseExample.notFound),
        )
      }
    }
  }
}

/**
 * `POST [pattern]`: restores the archive state of the entity to Active and answers with an [UnarchiveResponse].
 * [capability] guards the route, `CoreCapability.WriteData` unless the project names its own key.
 */
fun <Id> Route.unarchiveEntityRoute(
  pattern: UrlPattern.Relative1,
  label: EntityRouteLabel,
  tenantId: suspend RoutingContext.() -> Tenant.Id,
  idFromPath: Parameters.() -> Id,
  unarchive: suspend (Id, Tenant.Id) -> UnarchiveResult,
  capability: Capability = CoreCapability.WriteData,
) {
  route(pattern) {
    authorize(capability) {
      post {
        featureFlags().handle()

        val callerTenantId = tenantId()
        val result = unarchive(call.parameters.idFromPath(), callerTenantId)

        call.respond<UnarchiveResponse>(UnarchiveResponse.createFor(result))
      }.doc {
        summary = "Unarchive ${label.article} ${label.singular}"
        description = "Restores the archive state of the ${label.singular} to Active. ${requiresSentence(capability)}"

        noRequestBody()

        responding<UnarchiveResponse>(
          successExample = UnarchiveResponseExample.good,
          failureExamples = listOf(UnarchiveResponseExample.alreadyActive, UnarchiveResponseExample.notFound),
        )
      }
    }
  }
}

/**
 * `GET [pattern]`: answers with a [FoundResponse] of the preview [T] of the entity; `bad/NotFound` when [findPreview] returns `null`.
 * Requires [CoreCapability.ReadData].
 */
inline fun <reified T, Id> Route.previewEntityRoute(
  pattern: UrlPattern.Relative1,
  label: EntityRouteLabel,
  noinline tenantId: suspend RoutingContext.() -> Tenant.Id,
  noinline idFromPath: Parameters.() -> Id,
  noinline findPreview: suspend (Id, Tenant.Id) -> T?,
  successExample: FoundResponse<T>,
) {
  route(pattern) {
    authorize(CoreCapability.ReadData) {
      get {
        featureFlags().handle()

        val callerTenantId = tenantId()
        val preview = findPreview(call.parameters.idFromPath(), callerTenantId)

        call.respond<FoundResponse<T>>(FoundResponse.createFor(preview))
      }.doc {
        summary = "Get the preview of ${label.article} ${label.singular} by ID"
        description = "Returns the projection another feature embeds, for a caller that holds the ${label.singular} id alone. Requires ReadData."

        responding<FoundResponse<T>>(
          successExample = successExample,
          failureExample = FoundResponseExample.notFound,
        )
      }
    }
  }
}

/**
 * The text names the unarchive path without its leading `api/` segment.
 */
@PublishedApi
internal fun archiveEntityDescription(
  unarchivePattern: UrlPattern.Relative1,
  label: EntityRouteLabel,
  referencedBy: Boolean,
  descriptionNote: String?,
  capability: Capability,
): String {
  require(unarchivePattern.value.startsWith("api/")) { "Unarchive pattern <${unarchivePattern.value}> does not start with api/" }

  return buildList {
    add("Sets the archive state of the ${label.singular} to Archived. Reversible via POST /${unarchivePattern.value.removePrefix("api/")}.")
    if (referencedBy) {
      add("Answers with the active records that still point at the ${label.singular}; they stay valid.")
    }
    descriptionNote?.let(::add)
    add(requiresSentence(capability))
  }.joinToString(" ")
}

/**
 * `Requires WriteData.`: the simple name of the class of [capability], a `data object` in every catalog of capabilities.
 */
private fun requiresSentence(capability: Capability): String = "Requires ${capability::class.simpleName}."
