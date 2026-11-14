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

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.modelcontextprotocol.kotlin.sdk.server.DnsRebindingProtection
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import it.neckar.ktor.KtorRoutes
import it.neckar.ktor.authorization.authorize
import it.neckar.ktor.proxy.toUrl
import it.neckar.ktor.server.openapi.ktor.doc
import it.neckar.open.http.Url
import it.neckar.open.version.VersionInformation
import it.neckar.projects.common.McpResource
import it.neckar.rest.openapi.OpenApiMetaData.Category
import it.neckar.rest.openapi.dsl.OperationId
import kotlinx.serialization.json.JsonObject

/**
 * The MCP endpoint of a service: stateless Streamable HTTP, every `POST` answered as JSON.
 * The service installs [installJsonWithMcpMessages], which serializes the answers.
 * With [McpAuthentication.OAuth] the endpoint also serves its [ProtectedResourceMetadata] at [ProtectedResourceMetadataPath].
 */
class McpRouting(
  /**
   * Reported as `serverInfo.name`.
   */
  val serverName: McpServerName,
  /**
   * Reported as `serverInfo.title` and as `resource_name` of the [ProtectedResourceMetadata].
   */
  val serverTitle: McpServerTitle,
  /**
   * Reported as `serverInfo.websiteUrl`: the public page of the product behind the endpoint.
   */
  val websiteUrl: Url.Absolute,
  /**
   * The `Host` header values the endpoint accepts; every other host is refused as DNS rebinding. See [mcpAllowedHosts].
   */
  val allowedHosts: List<String>,
  val authentication: McpAuthentication,
  /**
   * Adds the tools to the server created for each request; the tools read the caller from the [ApplicationCall].
   */
  val registerTools: Server.(ApplicationCall) -> Unit,
) : KtorRoutes {
  override fun Application.configure() {
    routing {
      route(Path) {
        install(RequestBodyLimit) {
          bodyLimit { MaxRequestBodyBytes }
        }
        install(DnsRebindingProtection) {
          allowedHosts = this@McpRouting.allowedHosts
          // No Origin check: remote connectors call from their own servers, and CORS already limits browsers to the service's hosts.
        }

        when (authentication) {
          McpAuthentication.None -> mcpPost()
          is McpAuthentication.OAuth -> {
            protectedResourceMetadata(authentication)
            install(AuthenticationChallengeHeader) {
              scopes = authentication.scopes
            }
            authenticate(*authentication.providers.map { it.value }.toTypedArray()) {
              when (val capability = authentication.capability) {
                null -> mcpPost()
                else -> authorize(capability) { mcpPost() }
              }
            }
          }
        }
      }
    }
  }

  private fun Route.mcpPost() {
    post {
      val transport = StreamableHttpServerTransport(StreamableHttpServerTransport.Configuration(enableJsonResponse = true)).also {
        it.setSessionIdGenerator(null)
      }
      val session = createServer(call).createSession(transport)
      try {
        transport.handleRequest(null, call)
      } finally {
        session.close()
      }
    }.doc {
      // Own category: the JSON-RPC messages stay out of the REST spec the frontend clients are generated from.
      category = OpenApiCategory
      operationId = OperationId("PostMcp")

      summary = "MCP endpoint for AI clients"
      description = "Answers the JSON-RPC messages of the Model Context Protocol over stateless Streamable HTTP: https://modelcontextprotocol.io/specification"
      request {
        body<String> {
          description = "A JSON-RPC message of the Model Context Protocol"
          example("initialize", """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"client","version":"1"}}}""")
        }
      }
      response("The JSON-RPC answer") {
        statusCode = HttpStatusCode.OK
        noExamples()
      }
    }
  }

  private fun Route.protectedResourceMetadata(authentication: McpAuthentication.OAuth) {
    get(ProtectedResourceMetadataSubPath) {
      call.respond<ProtectedResourceMetadata>(
        ProtectedResourceMetadata(
          resource = call.publicUrl(Path),
          authorizationServers = authentication.authorizationServers,
          bearerMethodsSupported = listOf("header"),
          resourceName = serverTitle.value,
          scopesSupported = authentication.scopes,
        )
      )
    }.doc {
      category = OpenApiCategory
      operationId = OperationId("GetMcpProtectedResourceMetadata")

      summary = "OAuth metadata of the MCP endpoint"
      description = "The OAuth 2.0 Protected Resource Metadata (RFC 9728): the authorization servers an MCP client obtains its access token from"
      // No body schema: the generator maps each JSON name to a Kotlin property, and RFC 9728 names differ from them.
      response("The metadata as JSON: resource, authorization_servers, bearer_methods_supported, resource_name, scopes_supported") {
        statusCode = HttpStatusCode.OK
        noExamples()
      }
    }
  }

  private fun createServer(call: ApplicationCall): Server {
    return Server(
      serverInfo = Implementation(name = serverName.value, version = VersionInformation.version, title = serverTitle.value, websiteUrl = websiteUrl.value),
      options = ServerOptions(
        capabilities = ServerCapabilities(
          tools = ServerCapabilities.Tools(listChanged = false),
          // Resources carry the HTML of MCP Apps (addMcpApp); a server without apps lists none.
          resources = ServerCapabilities.Resources(listChanged = false, subscribe = false),
          extensions = mapOf(mcpAppsExtension to JsonObject(emptyMap())),
        ),
      ),
    ) {
      registerTools(call)
    }
  }

  companion object {
    /**
     * Outside the REST endpoints of the service, documented in [OpenApiCategory].
     */
    const val Path: String = McpResource.Path

    private const val ProtectedResourceMetadataSubPath: String = ".well-known/oauth-protected-resource"

    /**
     * Where [McpAuthentication.OAuth] serves the [ProtectedResourceMetadata]; the `401` of [Path] names it.
     */
    const val ProtectedResourceMetadataPath: String = "$Path/$ProtectedResourceMetadataSubPath"

    /**
     * The largest JSON-RPC message [Path] reads; a larger one is answered `413`. A tool call with its arguments takes a few kilobytes.
     */
    const val MaxRequestBodyBytes: Long = 256L * 1024

    /**
     * The OpenAPI category of [Path].
     */
    val OpenApiCategory: Category = Category("mcp")
  }
}

/**
 * The public URL of [path] as the client addressed the service; behind Traefik the forwarded scheme and host.
 */
private fun ApplicationCall.publicUrl(path: String): Url.Absolute {
  return request.origin.toUrl() + path
}

/**
 * The scopes [AuthenticationChallengeHeader] names in `WWW-Authenticate`.
 */
private class AuthenticationChallengeHeaderConfig {
  var scopes: List<OAuthScope> = emptyList()
}

/**
 * Adds `WWW-Authenticate: Bearer resource_metadata=…, scope=…` to every `401` below [McpRouting.Path], so an MCP client finds the authorization server (RFC 9728) and the scopes to request.
 * The challenge of the authentication provider answers without it.
 */
private val AuthenticationChallengeHeader: RouteScopedPlugin<AuthenticationChallengeHeaderConfig> = createRouteScopedPlugin("McpAuthenticationChallengeHeader", ::AuthenticationChallengeHeaderConfig) {
  val scopeParameter = pluginConfig.scopes.takeIf { it.isNotEmpty() }?.joinToString(separator = " ", prefix = ", scope=\"", postfix = "\"") { it.value }.orEmpty()
  onCallRespond { call, body ->
    val status = (body as? OutgoingContent)?.status ?: call.response.status()
    if (status == HttpStatusCode.Unauthorized) {
      call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer resource_metadata=\"${call.publicUrl(McpRouting.ProtectedResourceMetadataPath).value}\"$scopeParameter")
    }
  }
}
