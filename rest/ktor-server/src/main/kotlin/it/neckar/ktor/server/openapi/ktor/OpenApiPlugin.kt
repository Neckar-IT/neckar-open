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
package it.neckar.ktor.server.openapi.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.neckar.ktor.get
import it.neckar.ktor.getParamSafe
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.authenticateIfRequired
import it.neckar.ktor.proxy.toUrl
import it.neckar.ktor.respondJson
import it.neckar.ktor.route
import it.neckar.ktor.server.openapi.ktor.message.OpenApiCategoriesResponse
import it.neckar.open.http.Url
import it.neckar.open.http.UrlPattern
import it.neckar.open.kotlin.serializers.toStringPretty
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.OpenApiTag
import it.neckar.rest.openapi.dsl.OperationId
import it.neckar.rest.openapi.transform.OpenApiDocumentTransformer


/**
 * Installs OpenAPI JSON endpoints for API documentation and TypeScript type generation.
 */
val OpenApiPlugin: ApplicationPlugin<OpenApiPluginConfiguration> = createApplicationPlugin(
  "OpenApiPlugin",
  {
    OpenApiPluginConfiguration()
  }) {
  val config = this.pluginConfig

  if (config.metadata == OpenApiMetaData.default) {
    throw IllegalStateException("OpenApiMetaData must be set")
  }

  val authenticationRequirement = config.authenticationRequirement
    ?: throw IllegalStateException("AuthenticationRequirement must be set explicitly (Required or None)")

  val documentSource = OpenApiDocumentSource(baseUrl = config.baseUrl, metadata = config.metadata.withoutInternalServers(), additionalTypes = config.additionalTypes, documentTransformers = config.documentTransformers)
  application.attributes.put(OpenApiDocumentSourceKey, documentSource)

  application.routing {
    val baseUrl = config.baseUrl
    val metadata = config.metadata

    route(baseUrl) {
      // Deny-by-default: the OpenAPI spec (incl. the admin category) and the Scalar UI expose the
      // full API surface, so every doc endpoint requires an access token.
      authenticateIfRequired(authenticationRequirement) {
        get(OpenApiEndpoints.Openapi) {
          val json = documentSource.generateAll(application.openApiRoutes())
          call.respondJson(json.toString())
        }.doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetOpenApiDocumentation")
          tag = OpenApiTag("openapi", "OpenAPI")

          summary = "Returns the OpenAPI documentation"
          description = "Returns the OpenAPI documentation for the REST API in JSON format."

          response("OpenAPI JSON") {
            noExamples()
          }
        }

        get(OpenApiEndpoints.Categories) {
          val categories = documentSource.categories(application.openApiRoutes())

          val message: OpenApiCategoriesResponse = OpenApiCategoriesResponse.good(categories.toList())
          call.respond<OpenApiCategoriesResponse>(message)
        }.doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetOpenApiCategories")
          tag = OpenApiTag("openapi", "OpenAPI")

          summary = "Returns the available categories"
          description = "Returns the available categories for the OpenAPI documentation."

          responding<OpenApiCategoriesResponse>(
            successExample = OpenApiCategoriesResponse.good(listOf(OpenApiMetaData.Category.Default, OpenApiMetaData.Category.Admin, OpenApiMetaData.Category.Meta))
          )
        }

        get(OpenApiEndpoints.ByCategory) {
          val categoryName = call.parameters.getParamSafe(OpenApiEndpoints.Parameters.category)

          val json = documentSource.generateCategory(
            category = OpenApiMetaData.Category(categoryName),
            routes = application.openApiRoutes(),
          )
          call.respondJson(json.toStringPretty())
        }.doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetOpenApiDocumentationByCategory")
          tag = OpenApiTag("openapi", "OpenAPI")

          summary = "Returns the OpenAPI documentation for a specific category"
          description = "Returns the OpenAPI documentation for a specific category in JSON format."

          request {
            parameterString(OpenApiEndpoints.Parameters.category) {
              description = "The name of the category to filter the routes by"
              example(exampleIdAsString = "default", example = "default")
            }
          }

          response("OpenAPI JSON") {
            noExamples()
          }
        }

        get(OpenApiEndpoints.OpenapiWithOrigin) {
          val detectedServer: Url.Absolute = call.request.origin.toUrl()
          val liveMetadata = metadata.copy(servers = listOf(detectedServer))

          val json = documentSource.taggedGenerator().generate(
            routes = application.openApiRoutes(),
            additionalTypes = emptyList(),
            metaData = liveMetadata,
          )

          call.respondJson(json.toString())
        }.doc {
          category = OpenApiMetaData.Category.Meta
          operationId = OperationId("GetOpenApiDocumentationWithOrigin")
          tag = OpenApiTag("openapi", "OpenAPI")

          summary = "Returns the OpenAPI documentation with auto-detected server URL"
          description = "Returns the OpenAPI documentation with the servers list derived from the " +
            "incoming request's origin (X-Forwarded-* headers or direct connection). Falls back " +
            "to the direct connection values when no forwarding headers are present."

          response("OpenAPI JSON") {
            noExamples()
          }
        }

        if (config.scalarUi == ScalarUiMode.Enabled) {
          get(OpenApiEndpoints.Api) {
            val specUrl = OpenApiEndpoints.openapiWithOriginFullUrl(baseUrl)
            val html = ScalarHtml.render(specUrl = specUrl, title = metadata.title, oAuth2Config = metadata.oAuth2Config)
            call.respondText(html, ContentType.Text.Html)
          }.doc {
            category = OpenApiMetaData.Category.Meta
            operationId = OperationId("GetApiReferenceUi")
            tag = OpenApiTag("openapi", "OpenAPI")
            summary = "Returns the Scalar API reference UI"
            description = "Returns an HTML page that renders the OpenAPI documentation using Scalar."
            response("HTML") {
              noExamples()
            }
          }

          get(OpenApiEndpoints.ApiByCategory) {
            val category = OpenApiMetaData.Category(call.parameters.getParamSafe(OpenApiEndpoints.Parameters.category))
            val specUrl = OpenApiEndpoints.byCategoryFullUrl(baseUrl, category)
            val html = ScalarHtml.render(specUrl = specUrl, title = "${metadata.title} — ${category.value}", oAuth2Config = metadata.oAuth2Config)
            call.respondText(html, ContentType.Text.Html)
          }.doc {
            category = OpenApiMetaData.Category.Meta
            operationId = OperationId("GetApiReferenceUiByCategory")
            tag = OpenApiTag("openapi", "OpenAPI")
            summary = "Returns the Scalar API reference UI for a specific category"
            description = "Returns an HTML page that renders the OpenAPI documentation for a specific category using Scalar."
            request {
              parameterString(OpenApiEndpoints.Parameters.category) {
                description = "The name of the category to filter the routes by"
                example(exampleIdAsString = "default", example = "default")
              }
            }
            response("HTML") {
              noExamples()
            }
          }
        }
      }
    }
  }
}

/**
 * Whether the Scalar-based HTML UI (`/doc/api`, `/doc/api/{category}`) is installed by the [OpenApiPlugin].
 */
enum class ScalarUiMode {
  Enabled,
  Disabled,
}

class OpenApiPluginConfiguration {
  /**
   * Configures the plugin using the provided service descriptor
   */
  fun serviceDescriptor(serviceDescriptor: ServiceDescriptor, servers: List<Url> = emptyList()) {
    baseUrl = Url.pattern(serviceDescriptor.apiPrefix)
    metadata = OpenApiMetaData.create(serviceDescriptor = serviceDescriptor, servers = servers)
  }

  /**
   * The additional types that are exported to the OpenAPI documentation - even if they are not used in the endpoints.
   */
  var additionalTypes: OpenApiAdditionalTypes = OpenApiAdditionalTypes.empty

  /**
   * The base URL that is used to create the endpoints for the OpenAPI documentation.
   */
  var baseUrl: UrlPattern.Relative0 = Url.pattern("")

  /**
   * The meta data
   */
  var metadata: OpenApiMetaData = OpenApiMetaData.default

  /**
   * Whether the Scalar-based HTML UI is installed. Defaults to [ScalarUiMode.Enabled].
   */
  var scalarUi: ScalarUiMode = ScalarUiMode.Enabled

  /**
   * Whether the doc endpoints require an access token. No default — every service sets this
   * explicitly: [AuthenticationRequirement.Required] (requires the access-token provider to be
   * installed) or [AuthenticationRequirement.None] (service installs no provider).
   */
  var authenticationRequirement: AuthenticationRequirement? = null

  /**
   * The deviations from the default OpenAPI mapping, applied in order to every document served; [protocolFingerprint] applies the same.
   */
  var documentTransformers: List<OpenApiDocumentTransformer> = OpenApiDocumentTransformer.Defaults
}
