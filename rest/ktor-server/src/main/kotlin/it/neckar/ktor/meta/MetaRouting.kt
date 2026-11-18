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
package it.neckar.ktor.meta

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.neckar.common.featureflags.FeatureFlag
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.KtorRoutes
import it.neckar.ktor.authenticateIfRequired
import it.neckar.ktor.dumpRoutes
import it.neckar.ktor.exemptFromServerSimulation
import it.neckar.ktor.get
import it.neckar.ktor.getProtocolVersionString
import it.neckar.ktor.health.exemptFromReadinessGating
import it.neckar.ktor.health.globalHealth
import it.neckar.ktor.proxy.toUrl
import it.neckar.ktor.server.openapi.ktor.doc
import it.neckar.open.http.UrlPattern
import it.neckar.open.time.formatUtcForDebug
import it.neckar.open.time.nowMillis
import it.neckar.open.version.VersionInformation
import it.neckar.problem.ExpectedApplicationError
import it.neckar.problem.ExpectedApplicationErrorExample
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.rest.ClientId
import it.neckar.rest.ClientRole
import it.neckar.rest.PlatformHeader
import it.neckar.rest.error.GlobalHealthExample
import it.neckar.rest.error.GlobalHealthResponse
import it.neckar.rest.meta.ClientIdResponse
import it.neckar.rest.meta.ClientRoleResponse
import it.neckar.rest.meta.MetaTagsExample
import it.neckar.rest.meta.MetaTagsResponse
import it.neckar.rest.meta.RuntimeContextDetailsExample
import it.neckar.rest.meta.RuntimeContextDetailsResponse
import it.neckar.rest.meta.ServiceInfo
import it.neckar.rest.meta.ServiceInfoExample
import it.neckar.rest.meta.ServiceInfoResponse
import it.neckar.rest.meta.toDetails
import it.neckar.rest.meta.toMetaTags
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.OpenApiTag
import it.neckar.rest.openapi.dsl.OperationId
import it.neckar.rest.tracking.findClientId
import it.neckar.rest.tracking.findClientRole
import it.neckar.rest.version.RestProtocolVersionProblemException
import it.neckar.runtime.context.RuntimeContext
import it.neckar.uuid.NanoId

/**
 * Tags for meta endpoints, grouped by function.
 */
private val HealthTag: OpenApiTag = OpenApiTag(name = "health", displayName = "Health")
private val DiagnosticsTag: OpenApiTag = OpenApiTag(name = "diagnostics", displayName = "Diagnostics")
private val ConfigurationTag: OpenApiTag = OpenApiTag(name = "configuration", displayName = "Configuration")

/**
 * Contains the routing for metadata.
 *
 * Deny-by-default: only the liveness/health endpoints consumed anonymously by Docker healthchecks
 * and external monitoring (`global-health`, `meta-tags`; `heartbeat` lives in [it.neckar.ktor.HeartbeatRouting])
 * stay public. Every other meta endpoint — including the `dump-*` diagnostics and `boom` — requires an
 * access token per [authenticationRequirement].
 */
class MetaRouting(
  /**
   * THe service descriptor
   */
  val service: ServiceDescriptor,
  /**
   * The base url for the meta-routing of the service.
   * E.g., `api/service-name`
   */
  val baseUrl: UrlPattern.Relative0,

  /**
   * Whether the non-whitelisted meta endpoints require an access token. No default — set
   * explicitly by the calling service.
   */
  val authenticationRequirement: AuthenticationRequirement,

  /**
   * If set, overrides the port of the service in the base URL
   */
  val portOverride: Port? = null,
) : KtorRoutes {

  override fun Application.configure() {
    val endpoints = MetaRoutingEndpoints.forBaseUrl(baseUrl)

    routing {
      // Public whitelist: consumed anonymously by Docker healthchecks and external monitoring.
      // Never gated: a gated global-health would turn a down dependency into a container restart.
      get(endpoints.GlobalHealth) {
        call.respond<GlobalHealthResponse>(GlobalHealthResponse.good(call.application.globalHealth()))
      }.exemptFromServerSimulation().exemptFromReadinessGating().doc {
        category = OpenApiMetaData.Category.Meta
        operationId = OperationId("GetGlobalHealth")
        tag = HealthTag

        summary = "Returns the global health of the system"
        responding<GlobalHealthResponse>(
          successExample = GlobalHealthResponse.good(GlobalHealthExample.unhealthy)
        )
      }

      get(endpoints.MetaTags) {
        val response = MetaTagsResponse.good(VersionInformation.toMetaTags())
        call.respond<MetaTagsResponse>(response)
      }.exemptFromServerSimulation().doc {
        category = OpenApiMetaData.Category.Meta
        operationId = OperationId("GetMetaTags")
        tag = ConfigurationTag

        summary = "Get the meta tags of the service"
        description = "Returns the meta tags (version info) of the service"

        responding<MetaTagsResponse>(
          MetaTagsResponse.good(
            MetaTagsExample.example1
          )
        )
      }

      authenticateIfRequired(authenticationRequirement) {
        get(endpoints.RuntimeContext) {
          val response = RuntimeContextDetailsResponse.good(RuntimeContext.current.toDetails())
          call.respond<RuntimeContextDetailsResponse>(response)
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetRuntimeContext")
          tag = DiagnosticsTag

          summary = "Dump the runtime context of the service"
          description = "Diagnostics: how the running process classified its own deployment — stage, execution environment, hostname, debug mode, unit-test mode. Browser clients read their own stage from the serve-time HTML injection, not from here."

          responding<RuntimeContextDetailsResponse>(
            RuntimeContextDetailsResponse.good(
              RuntimeContextDetailsExample.example1
            )
          )
        }

        get(endpoints.BasePath) {
          call.respond<String>("${service.serviceName} up and running @ ${VersionInformation.artifactIdentity}")
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetRoot")
          tag = HealthTag

          summary = "Check if the service is running"
          description = "Returns a simple message to check if the service is running"
          response("A simple message") {
            statusCode = HttpStatusCode.OK
            body<String> {
              noExamples()
            }
          }
        }

        get(endpoints.Version) {
          call.respond<String>(VersionInformation.versionAsStringVerbose)
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetVersion")
          tag = HealthTag

          summary = "Get the version of the service"
          description = "Returns the version of the service"
          response("The version of the service") {
            statusCode = HttpStatusCode.OK
            body<String> {
              noExamples()
            }
          }
        }

        get(endpoints.FeatureFlags_Available) {
          val response = FeatureFlagsResponse.good(FeatureFlag.available)
          call.respond<FeatureFlagsResponse>(response)
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetAvailableFeatureFlags")
          tag = ConfigurationTag

          summary = "Get the available feature flags"
          description = "Returns the list of available feature flags with their keys and descriptions"

          responding<FeatureFlagsResponse>(
            FeatureFlagsResponse.good(FeatureFlag.available)
          )
        }

        get(endpoints.ServiceInfo) {
          val serviceInfo = ServiceInfo(
            serviceName = service.serviceName.value,
            version = VersionInformation.version,
            versionInfo = VersionInformation.toMetaTags(),
            runtimeContext = RuntimeContext.current.toDetails(),
            health = call.application.globalHealth(),
          )
          val response = ServiceInfoResponse.good(serviceInfo)
          call.respond<ServiceInfoResponse>(response)
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetServiceInfo")
          tag = ConfigurationTag

          summary = "Get the service info"
          description = "Returns identity, build provenance, runtime classification and health of the service"

          responding<ServiceInfoResponse>(
            ServiceInfoResponse.good(
              ServiceInfoExample.example1
            )
          )
        }

        get(endpoints.Version_VerifyClientVersion) {
          val versionString = getProtocolVersionString(call.request.headers)

          //The check needs real values on both sides: "unknown" means no deploy metadata is
          //injected (local development) - skip instead of failing spuriously
          val bothVersionsResolved = versionString.trim() != VersionInformation.UnknownValue &&
            VersionInformation.gitHash != VersionInformation.UnknownValue
          if (bothVersionsResolved && versionString.trim() != VersionInformation.gitHash) {
            throw RestProtocolVersionProblemException.invalidClientVersion(VersionInformation.gitHash, versionString)
          }
          call.respondText(versionString)
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetVerifyClientVersion")
          tag = HealthTag

          summary = "Verifies the client version"
          description = "Returns 400 if the client does not provide the exactly matching version"
          response("The version of the service") {
            statusCode = HttpStatusCode.OK
            body<String>()
          }
          // The block documents the answer to a protocol version conflict; it sends nothing.
          @Suppress("Status4xxIsAProtocolError")
          response("If the version does not match") {
            statusCode = HttpStatusCode.BadRequest
            body<ExpectedApplicationError>{
              example("error", ExpectedApplicationErrorExample.exampleError)
            }
          }
        }

        get(endpoints.Client_Role) {
          call.respond<ClientRoleResponse>(ClientRoleResponse.good(call.request.headers.findClientRole))
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetClientRole")
          tag = ConfigurationTag

          summary = "Returns the client role"
          description = "Returns the client role from the ${PlatformHeader.ClientRole.headerName} header. The data is null if the header is missing."
          responding<ClientRoleResponse>(
            ClientRoleResponse.good(ClientRole.UI)
          )
        }

        get(endpoints.Client_Id) {
          call.respond<ClientIdResponse>(ClientIdResponse.good(call.request.headers.findClientId))
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetClientId")
          tag = ConfigurationTag

          summary = "Returns the client id"
          description = "Returns the client ID from the ${PlatformHeader.ClientId.headerName} header. The data is null if the header is missing."
          responding<ClientIdResponse>(
            ClientIdResponse.good(ClientId(NanoId("ECg7BpWuYYZx92WppGHYy")))
          )
        }

        get(endpoints.Boom) {
          throw IllegalStateException("Something is broken!")
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetBoom")
          tag = DiagnosticsTag

          summary = "Throws an exception to test the error handling"
          description = "Throws an exception to test the error handling"

          noResponseBody()
        }

        get(endpoints.DumpRoutes) {
          call.respond<String>(call.application.dumpRoutes(call))
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetDumpRoutes")
          tag = DiagnosticsTag

          summary = "Dump the routes of the service"
          description = "Returns the routes of the service"
          response("The routes of the service") {
            statusCode = HttpStatusCode.OK
            body<String> {
              noExamples()
            }
          }
        }

        get(endpoints.DumpCookies) {
          val cookiesAsString = call.request.cookies.rawCookies.map { "${it.key}=${it.value}" }.joinToString("\n")

          call.response.cookies.append("Test-Cookie", "Test-Value @ ${nowMillis().formatUtcForDebug()}")
          call.respondText("Cookies:\n$cookiesAsString")
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetDumpCookies")
          tag = DiagnosticsTag

          summary = "Dump the cookies of the request"
          response("The cookies of the request") {
            statusCode = HttpStatusCode.OK
            body<String> {
              noExamples()
            }
          }
        }

        get(endpoints.DumpProxy) {
          val origin = call.request.origin
          val local = call.request.local
          val resolvedUrl = origin.toUrl()

          val forwardedHeaders = listOf(
            "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Port",
            "X-Forwarded-Proto", "X-Forwarded-Prefix",
            "Forwarded", "X-Real-IP",
          )

          call.respondText(buildString {
            appendLine("Reverse Proxy Diagnostics")
            appendLine("=========================")
            appendLine()
            appendLine("Resolved public URL: $resolvedUrl")
            appendLine()
            appendLine("Origin (after forwarded-header processing):")
            appendLine("  scheme:     ${origin.scheme}")
            appendLine("  serverHost: ${origin.serverHost}")
            appendLine("  serverPort: ${origin.serverPort}")
            appendLine()
            appendLine("Local (direct connection):")
            appendLine("  scheme:     ${local.scheme}")
            appendLine("  localAddress: ${local.localAddress}")
            appendLine("  localPort:  ${local.localPort}")
            appendLine()
            appendLine("Forwarded headers:")
            for (header in forwardedHeaders) {
              val value = call.request.headers[header]
              appendLine("  $header: ${value ?: "(not present)"}")
            }
          })
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetDumpProxy")
          tag = DiagnosticsTag

          summary = "Dump reverse proxy diagnostics"
          description = "Shows the resolved public URL, the origin (after X-Forwarded-* processing), the direct local connection, and all forwarding-related headers. Useful for diagnosing proxy configuration."
          response("Proxy diagnostics") {
            statusCode = HttpStatusCode.OK
            body<String> {
              noExamples()
            }
          }
        }

        get(endpoints.DumpHeaders) {

          call.respond<String>(buildString {
            appendLine("Dumping headers for request to ${call.request.uri}")
            appendLine()
            appendLine("origin: ${call.request.origin}")
            appendLine("origin.localAddress: ${call.request.origin.localAddress}")
            appendLine("origin.localPort: ${call.request.origin.localPort}")
            appendLine("origin.serverHost: ${call.request.origin.serverHost}")
            appendLine("origin.serverPort: ${call.request.origin.serverPort}")
            appendLine("origin.uri: ${call.request.origin.uri}")

            appendLine()
            appendLine()
            appendLine("Headers:")
            appendLine("------------------------------")
            appendLine(call.request.headers.entries().joinToString("\n") { "${it.key}=${it.value.joinToString(",")}" })
          })
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetDumpHeaders")
          tag = DiagnosticsTag

          summary = "Dump the headers of the request"
          response("The headers of the request") {
            statusCode = HttpStatusCode.OK
            body<String> {
              noExamples()
            }
          }
        }

        get(endpoints.DumpRequest) {

          call.respond<String>(buildString {
            appendLine("Dumping request for request to ${call.request.uri}")
            appendLine()

            val localRequest = call.request.local
            val originRequest = call.request.origin

            appendLine("local:")
            appendLine("  uri: ${localRequest.uri}")
            appendLine("  localAddress: ${localRequest.localAddress}")
            appendLine("  localPort: ${localRequest.localPort}")
            // No remoteHost or localHost here: both call InetSocketAddress.getHostName(), a
            // blocking reverse lookup on the event-loop thread.
            appendLine("  remoteAddress: ${localRequest.remoteAddress}")
            appendLine("  remotePort: ${localRequest.remotePort}")
            appendLine("  schema: ${localRequest.scheme}")
            appendLine("  method: ${localRequest.method}")
            appendLine("  version: ${localRequest.version}")
            appendLine()

            appendLine("origin:")
            appendLine("  uri: ${originRequest.uri}")
            appendLine("  serverHost: ${originRequest.serverHost}")
            appendLine("  serverPort: ${originRequest.serverPort}")
            appendLine("  remoteAddress: ${originRequest.remoteAddress}")
            appendLine("  remotePort: ${originRequest.remotePort}")
            appendLine("  uri: ${originRequest.uri}")
            appendLine("  scheme: ${originRequest.scheme}")
            appendLine("  method: ${originRequest.method.value}")
            appendLine("  version: ${originRequest.version}")
            appendLine()
          })
        }.exemptFromServerSimulation().doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetDumpRequest")
          tag = DiagnosticsTag

          summary = "Dump the headers of the request"
          response("The request information") {
            statusCode = HttpStatusCode.OK
            body<String> {
              noExamples()
            }
          }
        }
      }
    }
  }
}
