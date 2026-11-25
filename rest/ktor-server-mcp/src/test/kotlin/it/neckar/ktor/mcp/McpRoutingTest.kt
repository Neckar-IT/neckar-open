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
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import it.neckar.ktor.configure
import it.neckar.ktor.server.openapi.ktor.openApiRoutes
import it.neckar.ktor.server.openapi.ktor.verifyOpenApiDocComplete
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.test.testApplicationWithBuildTimeout
import it.neckar.runtime.context.DeploymentStage
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

@DisableLogging
class McpRoutingTest {
  @Test
  fun `initialize, tools-list and tools-call answer through the MCP client`() = testApplicationWithBuildTimeout {
    configureMcp()

    // The test engine sends no Host header of its own; the DNS rebinding protection refuses a request without one.
    val mcpClient = createClient { install(SSE) }.mcpStreamableHttp("http://localhost${McpRouting.Path}") {
      header(HttpHeaders.Host, "localhost")
    }
    try {
      assertThat(mcpClient.serverVersion?.name).isEqualTo("example")
      assertThat(mcpClient.serverVersion?.title).isEqualTo("Example Server")
      assertThat(mcpClient.serverVersion?.websiteUrl).isEqualTo("https://example.com")

      val tools = mcpClient.listTools().tools
      assertThat(tools.map { it.name }).containsExactly(HelloToolName)
      assertThat(tools.single().annotations?.readOnlyHint).isEqualTo(true)

      val result = mcpClient.callTool(name = HelloToolName, arguments = mapOf("name" to "Anna"))
      assertThat((result.content.single() as TextContent).text).isEqualTo("Hello Anna, this is example (Demo).")
      val structuredContent = result.structuredContent
      assertThat(structuredContent).isNotNull()
      assertThat(structuredContent!!["greeting"]!!.jsonPrimitive.content).isEqualTo("Hello Anna, this is example (Demo).")
      assertThat(structuredContent["deploymentStage"]!!.jsonPrimitive.content).isEqualTo("Demo")

      val resultWithoutName = mcpClient.callTool(name = HelloToolName, arguments = emptyMap())
      assertThat((resultWithoutName.content.single() as TextContent).text).isEqualTo("Hello there, this is example (Demo).")
    } finally {
      mcpClient.close()
    }
  }

  @Test
  fun `hello refuses a non-string name and an unknown argument`() = testApplicationWithBuildTimeout {
    configureMcp()

    val mcpClient = createClient { install(SSE) }.mcpStreamableHttp("http://localhost${McpRouting.Path}") {
      header(HttpHeaders.Host, "localhost")
    }
    try {
      val resultWithNumber = mcpClient.callTool(name = HelloToolName, arguments = mapOf("name" to 42))
      assertThat(resultWithNumber.isError).isEqualTo(true)
      assertThat((resultWithNumber.content.single() as TextContent).text).contains("name must be a non-blank string, found a number")

      val resultWithUnknownArgument = mcpClient.callTool(name = HelloToolName, arguments = mapOf("nmae" to "Anna"))
      assertThat(resultWithUnknownArgument.isError).isEqualTo(true)
      assertThat((resultWithUnknownArgument.content.single() as TextContent).text).contains("hello takes only [name] but got [nmae]")
    } finally {
      mcpClient.close()
    }
  }

  @Test
  fun `the response carries no explicit null`() = testApplicationWithBuildTimeout {
    configureMcp()

    val response = postInitialize(host = "localhost")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    val body = response.bodyAsText()
    assertThat(body).contains("\"name\":\"example\"")
    assertThat(body).doesNotContain("null")
  }

  @Test
  fun `a message of the maximum size is read, one byte more is refused with 413`() = testApplicationWithBuildTimeout {
    configureMcp()

    assertThat(postInitialize(host = "localhost", bodySize = McpRouting.MaxRequestBodyBytes).status).isEqualTo(HttpStatusCode.OK)
    assertThat(postInitialize(host = "localhost", bodySize = McpRouting.MaxRequestBodyBytes + 1).status).isEqualTo(HttpStatusCode.PayloadTooLarge)
  }

  @Test
  fun `an allowed host with a port is accepted`() = testApplicationWithBuildTimeout {
    configureMcp()

    assertThat(postInitialize(host = "127.0.0.1:10005").status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `a foreign host is refused`() = testApplicationWithBuildTimeout {
    configureMcp()

    assertThat(postInitialize(host = "evil.example").status).isEqualTo(HttpStatusCode.Forbidden)
  }

  @Test
  fun `a GET answers 405, the endpoint opens no SSE stream`() = testApplicationWithBuildTimeout {
    configureMcp()

    val response = client.get(McpRouting.Path) {
      header(HttpHeaders.Host, "localhost")
      header(HttpHeaders.Accept, "text/event-stream")
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.MethodNotAllowed)
  }

  @Test
  fun `the route is documented in its own OpenAPI category`() = testApplicationWithBuildTimeout {
    configureMcp()
    startApplication()

    assertThat(application.openApiRoutes(McpRouting.OpenApiCategory).map { it.operationId?.value }).containsExactly("PostMcp")
    application.verifyOpenApiDocComplete()
  }

  private fun ApplicationTestBuilder.configureMcp() {
    application {
      installJsonWithMcpMessages {
        explicitNulls = true
      }
      McpRouting(serverName = ExampleServerName, serverTitle = McpServerTitle("Example Server"), websiteUrl = Url.Absolute("https://example.com"), allowedHosts = listOf("localhost", "127.0.0.1"), authentication = McpAuthentication.None, instructions = null, prompts = emptyList()) {
        addHelloTool(ExampleServerName, DeploymentStage.Demo)
      }.configure(this)
    }
  }

  /**
   * Posts `initialize`; with [bodySize] the client name is padded so the message has exactly that many bytes.
   */
  private suspend fun ApplicationTestBuilder.postInitialize(host: String, bodySize: Long? = null): HttpResponse {
    //language=JSON
    val template = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"%s","version":"1"}}}"""
    val name = bodySize?.let { "x".repeat((it - template.replace("%s", "").length).toInt()) } ?: "test"
    return client.post(McpRouting.Path) {
      header(HttpHeaders.Host, host)
      header(HttpHeaders.Accept, "application/json, text/event-stream")
      contentType(ContentType.Application.Json)
      setBody(template.replace("%s", name))
    }
  }

  private companion object {
    val ExampleServerName: McpServerName = McpServerName("example")
  }
}
