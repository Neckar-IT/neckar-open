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
import assertk.assertions.isEqualTo
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.testing.*
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import it.neckar.ktor.authorization.installCapabilityChecks
import it.neckar.ktor.configure
import it.neckar.ktor.jwt.AuthProviderName
import it.neckar.ktor.status.installStatusPages
import it.neckar.open.authorization.Capability
import it.neckar.open.authorization.CoreCapability
import it.neckar.open.authorization.UserCapabilities
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.test.testApplicationWithBuildTimeout
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@DisableLogging
class McpRoutingOAuthTest {
  @Test
  fun `a request without token gets 401 naming the resource metadata`() = testApplicationWithBuildTimeout {
    configureMcp()

    val response = postInitialize(authorization = null)

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(response.headers.getAll(HttpHeaders.WWWAuthenticate)).isEqualTo(listOf("""Bearer resource_metadata="http://localhost/api/mcp/.well-known/oauth-protected-resource", scope="openid profile""""))
  }

  @Test
  fun `a request with a rejected token gets 401 naming the resource metadata`() = testApplicationWithBuildTimeout {
    configureMcp()

    val response = postInitialize(authorization = "Bearer wrong")

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(response.headers.getAll(HttpHeaders.WWWAuthenticate)).isEqualTo(listOf("""Bearer resource_metadata="http://localhost/api/mcp/.well-known/oauth-protected-resource", scope="openid profile""""))
  }

  @Test
  fun `the resource metadata names the authorization server without a token`() = testApplicationWithBuildTimeout {
    configureMcp()

    val response = client.get(McpRouting.ProtectedResourceMetadataPath) {
      header(HttpHeaders.Host, "localhost")
    }

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    val body = response.bodyAsText()
    // RFC 9728 names and plain URL strings, as an MCP client reads them.
    assertThat(body.replace(Regex("\\s"), "")).contains(""""resource":"http://localhost/api/mcp","authorization_servers":["https://auth.example/realms/example"]""")
    assertThat(Json.decodeFromString<ProtectedResourceMetadata>(body)).isEqualTo(
      ProtectedResourceMetadata(
        resource = Url.Absolute("http://localhost/api/mcp"),
        authorizationServers = listOf(Url.Absolute("https://auth.example/realms/example")),
        bearerMethodsSupported = listOf("header"),
        resourceName = "Example Server",
        scopesSupported = listOf(OAuthScope("openid"), OAuthScope("profile")),
      )
    )
  }

  @Test
  fun `a tool reads the caller of an authenticated request`() = testApplicationWithBuildTimeout {
    configureMcp()

    val mcpClient = createClient { install(SSE) }.mcpStreamableHttp("http://localhost${McpRouting.Path}") {
      header(HttpHeaders.Host, "localhost")
      header(HttpHeaders.Authorization, "Bearer $ValidToken")
    }
    try {
      val result = mcpClient.callTool(name = "whoami", arguments = emptyMap())
      assertThat((result.content.single() as TextContent).text).isEqualTo("anna")
    } finally {
      mcpClient.close()
    }
  }

  @Test
  fun `a token of the first provider is accepted`() = testApplicationWithBuildTimeout {
    configureMcp()

    val response = postInitialize(authorization = "Bearer $ValidMcpResourceToken")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
  }

  @Test
  fun `a token without the capability gets 403`() = testApplicationWithBuildTimeout {
    configureMcp()

    val response = postInitialize(authorization = "Bearer $TokenWithoutCapability")

    assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
    assertThat(response.bodyAsText()).contains(CoreCapability.ReadData.key.value)
  }

  @Test
  fun `without a capability every accepted token reaches the tools`() = testApplicationWithBuildTimeout {
    configureMcp(capability = null)

    val response = postInitialize(authorization = "Bearer $TokenWithoutCapability")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
  }

  private fun ApplicationTestBuilder.configureMcp(capability: Capability? = CoreCapability.ReadData) {
    application {
      installStatusPages(LoggerFactory.getLogger(McpRoutingOAuthTest::class.java))
      installCapabilityChecks { call ->
        when (call.principal<UserIdPrincipal>()?.name) {
          "anna" -> UserCapabilities(setOf(CoreCapability.ReadData.key))
          else -> UserCapabilities(emptySet())
        }
      }
      install(Authentication) {
        provider(McpResourceTestProvider.value) {
          authenticate { context ->
            if (context.call.request.headers[HttpHeaders.Authorization] == "Bearer $ValidMcpResourceToken") {
              context.principal(UserIdPrincipal("anna"))
            } else {
              context.challenge("mcp-resource-test", AuthenticationFailedCause.InvalidCredentials) { challenge, call ->
                call.respondText("", status = HttpStatusCode.Unauthorized)
                challenge.complete()
              }
            }
          }
        }
        provider(TestProvider.value) {
          authenticate { context ->
            when (context.call.request.headers[HttpHeaders.Authorization]) {
              "Bearer $ValidToken" -> context.principal(UserIdPrincipal("anna"))
              "Bearer $TokenWithoutCapability" -> context.principal(UserIdPrincipal("bob"))
              // Answers like the Keycloak provider: an empty 401 without WWW-Authenticate.
              else -> context.challenge("test", AuthenticationFailedCause.InvalidCredentials) { challenge, call ->
                call.respondText("", status = HttpStatusCode.Unauthorized)
                challenge.complete()
              }
            }
          }
        }
      }
      installJsonWithMcpMessages {
        explicitNulls = true
      }
      McpRouting(
        serverName = McpServerName("example"),
        serverTitle = McpServerTitle("Example Server"),
        websiteUrl = Url.Absolute("https://example.com"),
        allowedHosts = listOf("localhost"),
        authentication = McpAuthentication.OAuth(providers = listOf(McpResourceTestProvider, TestProvider), authorizationServers = listOf(Url.Absolute("https://auth.example/realms/example")), scopes = listOf(OAuthScope("openid"), OAuthScope("profile")), capability = capability),
instructions = null,
prompts = emptyList(),
      ) { call ->
        addTool(name = "whoami", description = "Names the caller", inputSchema = ToolSchema()) {
          CallToolResult(content = listOf(TextContent(call.principal<UserIdPrincipal>()!!.name)))
        }
      }.configure(this)
    }
  }

  private suspend fun ApplicationTestBuilder.postInitialize(authorization: String?): HttpResponse {
    return client.post(McpRouting.Path) {
      header(HttpHeaders.Host, "localhost")
      authorization?.let { header(HttpHeaders.Authorization, it) }
      header(HttpHeaders.Accept, "application/json, text/event-stream")
      contentType(ContentType.Application.Json)
      //language=JSON
      setBody(
        """
          {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}
        """.trimIndent()
      )
    }
  }

  private companion object {
    val TestProvider: AuthProviderName = AuthProviderName("mcp-test")
    val McpResourceTestProvider: AuthProviderName = AuthProviderName("mcp-resource-test")
    const val ValidMcpResourceToken: String = "mcp-resource-token"
    const val ValidToken: String = "valid"
    const val TokenWithoutCapability: String = "without-capability"
  }
}
