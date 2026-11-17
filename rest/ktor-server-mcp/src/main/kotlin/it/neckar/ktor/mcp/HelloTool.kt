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
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import it.neckar.runtime.context.DeploymentStage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Name of the tool [addHelloTool] adds.
 */
const val HelloToolName: String = "hello"

private const val NameArgument: String = "name"

/**
 * Adds the read-only tool [HelloToolName]: it greets the caller and names [serverName] and [deploymentStage], reads no data and writes nothing.
 */
fun Server.addHelloTool(serverName: McpServerName, deploymentStage: DeploymentStage) {
  val schema = ToolSchema(
    properties = buildJsonObject {
      putJsonObject(NameArgument) {
        put("type", "string")
        put("description", "Name to greet; the greeting says \"there\" without it.")
      }
    },
  )
  addTool(
    name = HelloToolName,
    description = "Greets the caller and names the $serverName instance that answered.",
    inputSchema = schema,
    toolAnnotations = ToolAnnotations(readOnlyHint = true),
  ) { request ->
    val name: String? = request.strictArguments(HelloToolName, schema).optionalNonBlankString(NameArgument)
    val greeting = "Hello ${name ?: "there"}, this is $serverName (${deploymentStage.name})."
    CallToolResult(
      content = listOf(TextContent(greeting)),
      structuredContent = buildJsonObject {
        put("greeting", greeting)
        put("deploymentStage", deploymentStage.name)
      },
    )
  }
}
