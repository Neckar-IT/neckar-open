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
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import it.neckar.ktor.configure
import it.neckar.open.authorization.Capability
import it.neckar.open.authorization.CoreCapability
import it.neckar.open.authorization.UserCapabilities
import it.neckar.open.http.Url
import it.neckar.open.tenancy.Tenant
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.test.testApplicationWithBuildTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test

@DisableLogging
class McpToolInvocationTest {
  private val access = McpToolAccesses(readScope = OAuthScope("example:read"), writeScope = OAuthScope("example:write"))

  private val reader = TestCaller(scopes = McpGrantedScopes.Granted(setOf(OAuthScope("example:read"))), capabilitySet = setOf(CoreCapability.ReadData))

  private val writer = TestCaller(scopes = McpGrantedScopes.Granted(access.scopes.toSet()), capabilitySet = setOf(CoreCapability.ReadData, CoreCapability.WriteData))

  @Test
  fun `each access maps to the annotations a client asks by`() = testApplicationWithBuildTimeout {
    withMcpClient(writer) { mcpClient ->
      val annotations = mcpClient.listTools().tools.associate { it.name to (it.annotations?.readOnlyHint to it.annotations?.destructiveHint) }
      assertThat(annotations[ReadTool]).isEqualTo(true to null)
      assertThat(annotations[CreateTool]).isEqualTo(false to false)
      assertThat(annotations[ChangeTool]).isEqualTo(false to true)
    }
  }

  @Test
  fun `a caller with every scope and capability runs each tool`() = testApplicationWithBuildTimeout {
    withMcpClient(writer) { mcpClient ->
      listOf(ReadTool, CreateTool, ChangeTool).forEach { tool ->
        val result = mcpClient.callTool(name = tool, arguments = emptyMap())
        assertThat(result.isError, name = result.text()).isNotEqualTo(true)
      }
    }
  }

  @Test
  fun `a token without the write scope reads and is refused every write naming the scope`() = testApplicationWithBuildTimeout {
    withMcpClient(reader.copy(capabilitySet = writer.capabilitySet)) { mcpClient ->
      assertThat(mcpClient.callTool(name = ReadTool, arguments = emptyMap()).isError).isNotEqualTo(true)
      listOf(CreateTool, ChangeTool).forEach { tool ->
        val result = mcpClient.callTool(name = tool, arguments = emptyMap())
        assertThat(result.isError).isEqualTo(true)
        assertThat(result.text()).isEqualTo("The access token lacks the scope example:write; sign in again and grant it")
      }
    }
  }

  @Test
  fun `a user without the capability WriteData is refused every write even with the access token of the service`() = testApplicationWithBuildTimeout {
    withMcpClient(reader.copy(scopes = McpGrantedScopes.AllOfTheUser)) { mcpClient ->
      assertThat(mcpClient.callTool(name = ReadTool, arguments = emptyMap()).isError).isNotEqualTo(true)
      val result = mcpClient.callTool(name = ChangeTool, arguments = emptyMap())
      assertThat(result.isError).isEqualTo(true)
      assertThat(result.text()).isEqualTo("The user anna.berger@example.org lacks the capability ${CoreCapability.WriteData.key}")
    }
  }

  @Test
  fun `a refused input becomes a tool error with its message`() = testApplicationWithBuildTimeout {
    withMcpClient(writer) { mcpClient ->
      val illegal = mcpClient.callTool(name = FailingTool, arguments = mapOf("failure" to "illegal"))
      assertThat(illegal.isError).isEqualTo(true)
      assertThat(illegal.text()).isEqualTo("failure: illegal")

      val product = mcpClient.callTool(name = FailingTool, arguments = mapOf("failure" to "frozen"))
      assertThat(product.isError).isEqualTo(true)
      assertThat(product.text()).isEqualTo("The record is frozen")
    }
  }

  @Test
  fun `the scopes of the claim scope are split at spaces`() {
    assertThat(McpGrantedScopes.Granted.parse("openid  example:read example:write").scopes.toList())
      .containsExactly(OAuthScope("openid"), OAuthScope("example:read"), OAuthScope("example:write"))
  }

  private suspend fun ApplicationTestBuilder.withMcpClient(caller: TestCaller, block: suspend (Client) -> Unit) {
    val invocation = McpToolInvocation(callerResolver = { caller }, isRefusedInput = { it is FrozenException })
    application {
      installJsonWithMcpMessages {}
      McpRouting(
        serverName = McpServerName("example"),
        serverTitle = McpServerTitle("Example Server"),
        websiteUrl = Url.Absolute("https://example.com"),
        allowedHosts = listOf("localhost"),
        authentication = McpAuthentication.None,
        instructions = null,
        prompts = emptyList(),
      ) { call ->
        val registrar = McpToolRegistrar(this, call, invocation, Json)
        registrar.tool(ReadTool, "Reads.", access.read, ToolSchema()) { _, _ -> registrar.read(Answer("read"), Answer.serializer()) }
        registrar.tool(CreateTool, "Creates.", access.create, ToolSchema()) { _, _ -> registrar.written(Answer("created"), Answer.serializer(), "id-1") }
        registrar.tool(ChangeTool, "Changes.", access.change, ToolSchema()) { _, _ -> registrar.written(Answer("changed"), Answer.serializer(), "id-1") }
        registrar.tool(FailingTool, "Fails.", access.read, ToolSchema(properties = buildJsonObject { stringProperty("failure", "How to fail.") })) { _, arguments ->
          when (arguments.requiredNonBlankString("failure")) {
            "illegal" -> throw IllegalArgumentException("failure: illegal")
            else -> throw FrozenException()
          }
        }
      }.configure(this)
    }

    // The test engine sends no Host header of its own; the DNS rebinding protection refuses a request without one.
    val mcpClient = createClient { install(SSE) }.mcpStreamableHttp("http://localhost${McpRouting.Path}") {
      header(HttpHeaders.Host, "localhost")
    }
    try {
      block(mcpClient)
    } finally {
      mcpClient.close()
    }
  }

  private fun CallToolResult.text(): String = (content.single() as TextContent).text

  private data class TestCaller(
    override val scopes: McpGrantedScopes,
    val capabilitySet: Set<Capability>,
  ) : McpCaller {
    override val loginName: String = "anna.berger@example.org"
    override val tenantId: Tenant.Id = Tenant.NeckarIT.id
    override val capabilities: UserCapabilities
      get() = UserCapabilities(capabilitySet.map { it.key }.toSet())
    override val oAuthClient: String = "example-mcp-claude"
  }

  private class FrozenException : RuntimeException("The record is frozen")

  @Serializable
  private data class Answer(val state: String)

  private companion object {
    const val ReadTool: String = "read_tool"
    const val CreateTool: String = "create_tool"
    const val ChangeTool: String = "change_tool"
    const val FailingTool: String = "failing_tool"
  }
}
