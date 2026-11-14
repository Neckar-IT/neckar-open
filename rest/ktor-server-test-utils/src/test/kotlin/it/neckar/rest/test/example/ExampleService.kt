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
package it.neckar.rest.test.example

import it.neckar.projects.common.ServiceName
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.neckar.ktor.KtorService
import it.neckar.ktor.baseUrl
import it.neckar.ktor.dumpRoutes
import it.neckar.ktor.installCallId
import it.neckar.ktor.installCallLogging
import it.neckar.ktor.installCompression
import it.neckar.ktor.installIgnoreTailingSlash
import it.neckar.ktor.installJson
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.metaRouting
import it.neckar.ktor.metricsRouting
import it.neckar.ktor.neckarITServerHeader
import it.neckar.ktor.servedByHeader
import it.neckar.ktor.restVersionNumberHeader
import it.neckar.ktor.server.openapi.ktor.OpenApiPlugin
import it.neckar.ktor.server.openapi.ktor.doc
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.http.Url
import it.neckar.open.version.Version
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.rest.RestRequest
import it.neckar.rest.RestResponse
import it.neckar.rest.openapi.OpenApiMetaData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Example service that shows how to configure and test a service
 */
class ExampleService(
  portOverride: Port? = null,
) : KtorService {
  /**
   * Usually declared in `buildSrc/src/main/kotlin/it.neckar.projects...` and copied into the project using the `ProjectConfigurationPlugin`.
   */
  override val serviceDescriptor: ServiceDescriptor = object : ServiceDescriptor {
    override val serviceName: ServiceName = ServiceName("Example Service")
    override val port: Port = Port(9999)
    override val apiPrefix: String = "api/example"
  }

  override val baseUrl: Url.Absolute = serviceDescriptor.baseUrl(portOverride)

  override fun Application.configure() {
    logger.info("Starting Example Service")
    logger.info("Env: ${System.getenv()}")

    /*
    * Service setup Modules
    * installing required plugins
    * */
    installPlugins()

    val apiUrl = Url.pattern("api/example")
    // This example service installs no access-token provider, so the meta/doc endpoints stay public.
    metaRouting(serviceDescriptor, apiUrl, authenticationRequirement = AuthenticationRequirement.None)
    metricsRouting(apiUrl)

    /*
     * Routes of the service
     * */
    myTestServiceRouting()

    logger.info(dumpRoutes())
  }

  private fun Application.myTestServiceRouting() {
    routing {
      get("/hello/world") {
        call.respond("Hello World!")
      }.doc {
        summary = "Hello World endpoints"
        description = "Returns a simple string that says hello"

        response("The message") {
          body<String> {
            example("sample-message", "Simple example")
          }
        }
      }

      accept(ContentType.Application.Json) {
        post("/foobar") {
          val fooBarRequest = call.receive<FooBarRequest>()
          val fooBar = fooBarRequest.foobar

          if (fooBar.shouldFail) {
            call.respond(FooBarResponse.bad())
          } else {
            call.respond(FooBarResponse.createFor(fooBar))
          }
        }.doc {
          requestBody<FooBarRequest> {
            example("foobar-example", FooBarRequest(FooBar("foo", "bar")))
          }
          responding<FooBarResponse>(
            successExample = FooBarResponse.good(FooBar("foo", "bar")),
            failureExample = FooBarResponse.bad()
          )
        }
      }
    }
  }

  /**
   * Module for initial server settings like headers
   * authentication and json support
   * */
  fun Application.installPlugins() {
    installCallId()
    installCallLogging()
    installCompression()

    installIgnoreTailingSlash()

    installJson { }

    install(DefaultHeaders) {
      neckarITServerHeader()
      restVersionNumberHeader()
      servedByHeader(serviceDescriptor.serviceName)
    }

    install(ConditionalHeaders) {
      //Set the ETag in the response header
      //This plugin skips the response automatically if the ETag is the same
    }

    install(OpenApiPlugin) {
      baseUrl = Url.pattern(serviceDescriptor.apiPrefix)
      metadata = OpenApiMetaData(
        "Example Service",
        "REST API of an example service",
        listOf(Url.absolute("https://example.neckar.it")),
        Version(1, 2, 3),
      )
      authenticationRequirement = AuthenticationRequirement.None
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.rest.test.example.ExampleService")
  }

  /*
   * Below are the REST request/response classes used by this simple service
   */
  @Serializable
  data class FooBar(
    val foo: String,
    val bar: String,
    val shouldFail: Boolean = false,
  )

  /**
   * Request object
   */
  @Serializable
  data class FooBarRequest(
    val foobar: FooBar,
  ) : RestRequest

  /**
   * Response for
   */
  @Serializable
  sealed interface FooBarResponse : RestResponse<FooBar> {

    @Serializable
    @SerialName("good")
    data class Good(override val data: FooBar) : FooBarResponse, RestResponse.GoodResponse<FooBar>

    @Serializable
    @SerialName("bad/Failed")
    data object Failed : FooBarResponse, RestResponse.BadResponse<FooBar>

    companion object {
      /**
       * Helper method to create a success object - with the correct type
       */
      fun good(data: FooBar): FooBarResponse = Good(data)

      /**
       * Helper method to create a failure object - with the correct type
       */
      fun bad(): FooBarResponse = Failed

      /**
       * Creates a success method for non-null results, failure for null results
       */
      fun createFor(data: FooBar?): FooBarResponse {
        return if (data != null) {
          good(data)
        } else {
          bad()
        }
      }
    }
  }
}
