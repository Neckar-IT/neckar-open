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

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * The extension a server declares in its capabilities to tell hosts that its tools carry MCP Apps.
 */
const val mcpAppsExtension: String = "io.modelcontextprotocol/ui"

/**
 * The MIME type a host renders as MCP App.
 */
const val mcpAppMimeType: String = "text/html;profile=mcp-app"

/**
 * Serves [html] as the MCP App at [uri]; a tool shows it through [mcpAppToolMeta].
 * The host runs it in a sandboxed iframe whose default CSP admits inline script and style and no network access.
 */
fun Server.addMcpApp(uri: McpAppUri, name: String, description: String, html: String) {
  addResource(uri = uri.value, name = name, description = description, mimeType = mcpAppMimeType) {
    ReadResourceResult(contents = listOf(TextResourceContents(text = html, uri = uri.value, mimeType = mcpAppMimeType)))
  }
}

/**
 * The `_meta` of a tool whose results a host shows in the MCP App at [uri].
 */
fun mcpAppToolMeta(uri: McpAppUri): JsonObject {
  return buildJsonObject {
    putJsonObject("ui") {
      put("resourceUri", uri.value)
    }
  }
}
