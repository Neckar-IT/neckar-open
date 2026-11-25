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
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

/**
 * A prompt of the server: a user picks it in the client, fills [arguments], and [message] becomes the user message the model works on.
 */
class McpPrompt(
  val name: String,
  val description: String,
  val arguments: List<PromptArgument>,
  /**
   * The user message for the given argument values; an argument the user left empty is absent.
   */
  val message: (arguments: Map<String, String>) -> String,
)

/**
 * Registers [prompt]; a required argument without value fails as an invalid request.
 */
fun Server.addPrompt(prompt: McpPrompt) {
  addPrompt(name = prompt.name, description = prompt.description, arguments = prompt.arguments) { request ->
    val values = request.arguments.orEmpty().filterValues { it.isNotBlank() }
    val missing = prompt.arguments.filter { it.required == true && it.name !in values }.map { it.name }
    require(missing.isEmpty()) { "The prompt ${prompt.name} needs $missing" }
    GetPromptResult(
      messages = listOf(PromptMessage(role = Role.User, content = TextContent(prompt.message(values)))),
      description = prompt.description,
    )
  }
}
