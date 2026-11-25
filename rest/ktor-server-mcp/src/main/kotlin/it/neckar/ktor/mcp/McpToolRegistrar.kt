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
package it.neckar.ktor.mcp

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import it.neckar.ktor.proxy.toUrl
import it.neckar.open.http.Url
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Adds the tools of a product to the [server] created for one MCP request; every tool runs through [invocation] as the caller of [call].
 */
class McpToolRegistrar<C : McpCaller>(
  val server: Server,
  private val call: ApplicationCall,
  private val invocation: McpToolInvocation<C>,
  private val json: Json,
) {
  /**
   * The address of [path] in the user interface under which the client addressed the service, e.g. `https://patterns.neckar.it/authors/…`.
   */
  fun uiUrl(path: String): Url.Absolute {
    return call.request.origin.toUrl() + path
  }

  /**
   * Adds the tool [name]. [activities] names every concept activity the tool carries out; `tools/list` shows them in `_meta`
   * under [McpToolActivity.MetaKey], so a product can check that its tools and its concepts cover each other.
   */
  fun tool(
    name: String,
    description: String,
    access: McpToolAccess,
    schema: ToolSchema,
    meta: JsonObject? = null,
    activities: List<McpToolActivity> = emptyList(),
    handler: suspend (caller: C, arguments: JsonObject) -> McpToolOutcome,
  ) {
    server.addTool(
      name = name,
      description = description,
      inputSchema = schema,
      toolAnnotations = when (access.kind) {
        McpToolAccess.Kind.Read -> ToolAnnotations(readOnlyHint = true)
        McpToolAccess.Kind.Create -> ToolAnnotations(readOnlyHint = false, destructiveHint = false)
        McpToolAccess.Kind.Change -> ToolAnnotations(readOnlyHint = false, destructiveHint = true)
      },
      meta = withActivities(meta, activities),
    ) { request ->
      invocation.invoke(call, name, access, schema, request, handler)
    }
  }

  /**
   * The outcome of a read tool: [value] as structured content and as its JSON text.
   */
  fun <T> read(value: T, serializer: KSerializer<T>): McpToolOutcome {
    return McpToolOutcome(structuredToolResult(value, serializer, json), changedRecordId = null)
  }

  /**
   * The outcome of a write tool that created or changed the record [changedRecordId].
   */
  fun <T> written(value: T, serializer: KSerializer<T>, changedRecordId: String): McpToolOutcome {
    return McpToolOutcome(structuredToolResult(value, serializer, json), changedRecordId = changedRecordId)
  }
}

/**
 * A group of tools on one subject, e.g. the catalog.
 */
fun interface McpToolGroup<C : McpCaller> {
  fun register(registrar: McpToolRegistrar<C>)
}
