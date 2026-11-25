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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import it.neckar.ktor.configure
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.test.testApplicationWithBuildTimeout
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

@DisableLogging
class McpAppsTest {
  @Test
  fun `a tool points to its MCP App, which the server declares and serves`() = testApplicationWithBuildTimeout {
    application {
      installJsonWithMcpMessages {
        explicitNulls = true
      }
      McpRouting(serverName = McpServerName("example"), serverTitle = McpServerTitle("Example Server"), websiteUrl = Url.Absolute("https://example.com"), allowedHosts = listOf("localhost"), authentication = McpAuthentication.None, instructions = null, prompts = emptyList()) {
        addMcpApp(ExampleApp, name = "Example", description = "Shows the example result", html = ExampleHtml)
        addTool(name = "example", description = "An example", inputSchema = ToolSchema(), meta = mcpAppToolMeta(ExampleApp)) {
          CallToolResult(content = emptyList())
        }
      }.configure(this)
    }

    val mcpClient = createClient { install(SSE) }.mcpStreamableHttp("http://localhost${McpRouting.Path}") {
      header(HttpHeaders.Host, "localhost")
    }
    try {
      assertThat(mcpClient.serverCapabilities?.extensions?.keys?.toList()).isEqualTo(listOf(mcpAppsExtension))

      val tool = mcpClient.listTools().tools.single()
      assertThat(tool.meta?.get("ui")?.jsonObject?.get("resourceUri")?.jsonPrimitive?.content).isEqualTo("ui://example/app")

      val resource = mcpClient.listResources().resources.single()
      assertThat(resource.uri).isEqualTo("ui://example/app")
      assertThat(resource.mimeType).isEqualTo("text/html;profile=mcp-app")

      val contents = mcpClient.readResource(ReadResourceRequest(ReadResourceRequestParams(uri = "ui://example/app"))).contents.single() as TextResourceContents
      assertThat(contents.mimeType).isEqualTo("text/html;profile=mcp-app")
      assertThat(contents.text).isEqualTo(ExampleHtml)
    } finally {
      mcpClient.close()
    }
  }

  @Test
  fun `an MCP App URI starts with ui`() {
    assertThat(runCatching { McpAppUri("https://example/app") }.exceptionOrNull()?.message).isNotNull().isEqualTo("An MCP App URI starts with ui:// but was 'https://example/app'")
  }

  private companion object {
    val ExampleApp: McpAppUri = McpAppUri("ui://example/app")
    const val ExampleHtml: String = "<!doctype html><p>example</p>"
  }
}
