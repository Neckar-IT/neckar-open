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
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import it.neckar.open.exceptions.NotFoundException
import it.neckar.open.tenancy.InvalidTenantException
import it.neckar.open.time.monotonicTimeSource
import kotlinx.serialization.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The answer of a tool handler: the result for the client and, for a write tool, the ID of the record it created or changed.
 */
data class McpToolOutcome(
  val result: CallToolResult,
  /**
   * `null` for a read tool.
   */
  val changedRecordId: String?,
) {
  companion object {
    /**
     * A refused input: [message] names the field, the cause and the next step.
     */
    fun error(message: String): McpToolOutcome {
      return McpToolOutcome(CallToolResult(content = listOf(TextContent(message)), isError = true), changedRecordId = null)
    }
  }
}

/**
 * Runs one tool call as the [McpCaller] of the request: checks scope and capability, answers a refused input as a tool error and writes one log line per call.
 */
class McpToolInvocation<C : McpCaller>(
  private val callerResolver: McpCallerResolver<C>,
  /**
   * Whether a product exception carries a message for the user, such as a record frozen by a workflow; [IllegalArgumentException], [NotFoundException],
   * [InvalidTenantException] and [McpCallerRefusedException] always do.
   */
  private val isRefusedInput: (Exception) -> Boolean = { false },
) {
  suspend fun invoke(
    call: ApplicationCall,
    toolName: String,
    access: McpToolAccess,
    schema: ToolSchema,
    request: CallToolRequest,
    handler: suspend (caller: C, arguments: JsonObject) -> McpToolOutcome,
  ): CallToolResult {
    val started = monotonicTimeSource.markNow()
    var caller: C? = null
    val outcome: McpToolOutcome = try {
      val resolved = callerResolver.resolve(call).also { caller = it }
      resolved.requireAccess(access)
      handler(resolved, request.strictArguments(toolName, schema))
    } catch (e: Exception) {
      when {
        // The messages the REST status pages send to the user interface for the same input.
        e is McpCallerRefusedException || e is IllegalArgumentException || e is NotFoundException || e is InvalidTenantException || isRefusedInput(e) ->
          McpToolOutcome.error(e.message ?: e::class.java.simpleName)

        else -> throw e
      }
    }

    val resultSize = outcome.result.content.sumOf { (it as? TextContent)?.text?.encodeToByteArray()?.size ?: 0 }
    logger.info(
      "MCP tool [$toolName] user [${caller?.loginName}] tenant [${caller?.tenantId}] client [${caller?.oAuthClient}] " +
        "arguments ${request.arguments} duration [${started.elapsedNow()}] result [$resultSize bytes] " +
        "isError [${outcome.result.isError == true}] changed [${outcome.changedRecordId}]"
    )
    return outcome.result
  }

  private fun C.requireAccess(access: McpToolAccess) {
    if (access.scope !in scopes) {
      throw McpCallerRefusedException("The access token lacks the scope ${access.scope}; sign in again and grant it")
    }
    if (access.capability !in capabilities) {
      throw McpCallerRefusedException("The user $loginName lacks the capability ${access.capability.key}")
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(McpToolInvocation::class.java)
  }
}
