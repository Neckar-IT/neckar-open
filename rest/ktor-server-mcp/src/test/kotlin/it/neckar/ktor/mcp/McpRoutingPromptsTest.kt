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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.server.testing.*
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import it.neckar.ktor.configure
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.test.testApplicationWithBuildTimeout
import org.junit.jupiter.api.Test

@DisableLogging
class McpRoutingPromptsTest {
  private val admitBook = McpPrompt(
    name = "admit_book",
    description = "Guides through the admission of a book.",
    arguments = listOf(PromptArgument(name = "title", description = "The title", required = true)),
    message = { arguments -> "Nimm das Buch ${arguments.getValue("title")} auf." },
  )

  @Test
  fun `initialize names the instructions and prompts-get renders the message`() = testApplicationWithBuildTimeout {
    application {
      installJsonWithMcpMessages {}
      McpRouting(
        serverName = McpServerName("example"),
        serverTitle = McpServerTitle("Example Server"),
        websiteUrl = Url.Absolute("https://example.com"),
        allowedHosts = listOf("localhost"),
        authentication = McpAuthentication.None,
        instructions = "The example server keeps books.",
        prompts = listOf(admitBook),
      ) { }.configure(this)
    }
    val mcpClient = createClient { install(SSE) }.mcpStreamableHttp("http://localhost${McpRouting.Path}") {
      header(io.ktor.http.HttpHeaders.Host, "localhost")
    }
    try {
      assertThat(mcpClient.serverInstructions).isEqualTo("The example server keeps books.")
      assertThat(mcpClient.listPrompts().prompts.map { it.name }).containsExactly("admit_book")

      val prompt = mcpClient.getPrompt(GetPromptRequest(GetPromptRequestParams(name = "admit_book", arguments = mapOf("title" to "Siddhartha"))))
      assertThat((prompt.messages.single().content as TextContent).text).isEqualTo("Nimm das Buch Siddhartha auf.")

      assertFailure { mcpClient.getPrompt(GetPromptRequest(GetPromptRequestParams(name = "admit_book", arguments = emptyMap()))) }
    } finally {
      mcpClient.close()
    }
  }
}
