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

import assertk.*
import assertk.assertions.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import it.neckar.rest.test.*
import it.neckar.ktor.client.bodyAsTextSuccess
import it.neckar.common.featureflags.FeatureFlags
import it.neckar.ktor.FeatureFlagsPlugin
import it.neckar.ktor.configure
import it.neckar.ktor.installJson
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.meta.MetaRouting
import it.neckar.ktor.proxy.installReverseProxySupport
import it.neckar.open.health.error.GlobalExceptionCollector
import it.neckar.open.http.Url
import it.neckar.open.version.VersionInformation
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import it.neckar.rest.ClientId
import it.neckar.rest.ClientRole
import it.neckar.rest.dataOrNull
import it.neckar.rest.error.GlobalHealth
import it.neckar.rest.meta.ClientIdResponse
import it.neckar.rest.meta.ClientRoleResponse
import it.neckar.rest.meta.RuntimeContextDetailsResponse
import it.neckar.rest.meta.ServiceInfoResponse
import it.neckar.rest.meta.toDetails
import it.neckar.rest.meta.toMetaTags
import it.neckar.rest.tracking.XClientId
import it.neckar.rest.tracking.XClientRole
import it.neckar.uuid.NanoId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import it.neckar.open.test.utils.DisableLogging
import it.neckar.runtime.context.RuntimeContext

/**
 *
 */
@DisableLogging
class MetaRoutingTest {
  @BeforeEach
  fun setUp() {
    GlobalExceptionCollector.clearErrors()
  }

  /**
   * No health layer is installed here, so there is no [it.neckar.open.health.ServiceStateHolder] to
   * project — `global-health` falls back to the uncaught-exception collector, which is empty.
   */
  @Test
  fun testGlobalHealth() = testApplication {
    application {
      installJson()

      MetaRouting(
        service = ServiceDescriptor("test-service", apiPrefix = "test-service", Port(1234)),
        baseUrl = Url.pattern("test-service/meta"),
        authenticationRequirement = AuthenticationRequirement.None,
      ).configure(this)
    }

    val client = createClient {
      install(ContentNegotiation) {
        json()
      }
    }

    client.get("test-service/meta") {
      accept(ContentType.Application.Json)
    }.let { response ->
      assertThat(response).isOk()
      assertThat(response.bodyAsTextSuccess()).startsWith("test-service up and running @")
    }

    client.get("test-service/meta/global-health") {
      accept(ContentType.Application.Json)

    }.let { response ->
      assertThat(response).isOk()
      val healthResponse = response.body<it.neckar.rest.error.GlobalHealthResponse>()

      assertThat(healthResponse).isNotNull()
      assertThat(healthResponse.dataOrNull()).isEqualTo(GlobalHealth.Healthy)
    }


  }

  @Test
  fun testClientId() = testApplication {
    application {
      installJson()

      MetaRouting(
        service = ServiceDescriptor("test-service", apiPrefix = "test-service", Port(1234)),
        baseUrl = Url.pattern("test-service/meta"),
        authenticationRequirement = AuthenticationRequirement.None,
      ).configure(this)
    }

    val client = createClient {
      install(ContentNegotiation) {
        json()
      }
    }

    client.get("test-service/meta/client/id") {
      accept(ContentType.Application.Json)
      header(HttpHeaders.XClientId, "ECg7BpWuYYZx92WppGHYy")
    }.let { response ->
      assertThat(response).isOk()
      assertThat(response.body<ClientIdResponse>().asGood().data).isEqualTo(ClientId(NanoId("ECg7BpWuYYZx92WppGHYy")))
    }

    client.get("test-service/meta/client/id") {
      accept(ContentType.Application.Json)
    }.let { response ->
      assertThat(response).isOk()
      assertThat(response.body<ClientIdResponse>().asGood().data).isNull()
    }
  }

  @Test
  fun testClientRole() = testApplication {
    application {
      installJson()

      MetaRouting(
        service = ServiceDescriptor("test-service", apiPrefix = "test-service", Port(1234)),
        baseUrl = Url.pattern("test-service/meta"),
        authenticationRequirement = AuthenticationRequirement.None,
      ).configure(this)
    }

    val client = createClient {
      install(ContentNegotiation) {
        json()
      }
    }

    client.get("test-service/meta/client/role") {
      accept(ContentType.Application.Json)
      header(HttpHeaders.XClientRole, ClientRole.UI.value)
    }.let { response ->
      assertThat(response).isOk()
      assertThat(response.body<ClientRoleResponse>().asGood().data).isEqualTo(ClientRole.UI)
    }

    client.get("test-service/meta/client/role") {
      accept(ContentType.Application.Json)
    }.let { response ->
      assertThat(response).isOk()
      assertThat(response.body<ClientRoleResponse>().asGood().data).isNull()
    }
  }

  @Test
  fun testDumpRoutesWithProxyHeaders() = testApplication {
    application {
      installJson()
      installReverseProxySupport()

      MetaRouting(
        service = ServiceDescriptor("test-service", apiPrefix = "test-service", Port(1234)),
        baseUrl = Url.pattern("test-service/meta"),
        authenticationRequirement = AuthenticationRequirement.None,
      ).configure(this)
    }

    val client = createClient {
      install(ContentNegotiation) {
        json()
      }
    }

    client.get("test-service/meta/dump-routes") {
      accept(ContentType.Text.Plain)
      header("X-Forwarded-Proto", "https")
      header("X-Forwarded-Host", "example.com")
      header("X-Forwarded-Port", "443")
    }.let { response ->
      assertThat(response).isOk()
      val routes = response.bodyAsTextSuccess()

      assertThat(routes).contains("https://example.com/test-service/meta/(method:GET)")
      assertThat(routes).contains("https://example.com/test-service/meta/dump-routes/(method:GET)")
      assertThat(routes).doesNotContain("http://localhost")
      assertThat(routes).doesNotContain(":1234")
    }
  }

  @Test
  fun testDumpRoutesWithProxyHeadersNonStandardPort() = testApplication {
    application {
      installJson()
      installReverseProxySupport()

      MetaRouting(
        service = ServiceDescriptor("test-service", apiPrefix = "test-service", Port(1234)),
        baseUrl = Url.pattern("test-service/meta"),
        authenticationRequirement = AuthenticationRequirement.None,
      ).configure(this)
    }

    val client = createClient {
      install(ContentNegotiation) {
        json()
      }
    }

    client.get("test-service/meta/dump-routes") {
      accept(ContentType.Text.Plain)
      header("X-Forwarded-Proto", "https")
      header("X-Forwarded-Host", "example.com")
      header("X-Forwarded-Port", "8443")
    }.let { response ->
      assertThat(response).isOk()
      val routes = response.bodyAsTextSuccess()

      assertThat(routes).contains("https://example.com:8443/test-service/meta/(method:GET)")
      assertThat(routes).contains("https://example.com:8443/test-service/meta/dump-routes/(method:GET)")
    }
  }

  @Test
  fun testServiceInfoEndpoint() = testApplication {
    application {
      installJson()

      MetaRouting(
        service = ServiceDescriptor("test-service", apiPrefix = "test-service", Port(1234)),
        baseUrl = Url.pattern("test-service/meta"),
        authenticationRequirement = AuthenticationRequirement.None,
      ).configure(this)
    }

    val client = createClient {
      install(ContentNegotiation) {
        json()
      }
    }

    client.get("test-service/meta/service-info") {
      accept(ContentType.Application.Json)
    }.let { response ->
      assertThat(response).isOk()
      val serviceInfoResponse = response.body<ServiceInfoResponse>()

      assertThat(serviceInfoResponse).isNotNull()
      val serviceInfoData = serviceInfoResponse.dataOrNull()
      assertThat(serviceInfoData).isNotNull()

      assertThat(serviceInfoData!!.serviceName).isEqualTo("test-service")
      assertThat(serviceInfoData.version).isNotEmpty()
      //No deploy metadata is injected in the test environment - the git info is unresolved
      assertThat(serviceInfoData.versionInfo).isEqualTo(VersionInformation.toMetaTags())
      assertThat(serviceInfoData.versionInfo.gitHash).isNull()
      assertThat(serviceInfoData.versionInfo.imageDigest).isNull()
      assertThat(serviceInfoData.health).isNotNull()

      //The embedded runtime context survives the round trip as a whole...
      assertThat(serviceInfoData.runtimeContext).isEqualTo(RuntimeContext.current.toDetails())
      //...and carries a resolved hostname - asserted on its own so a projection that dropped the
      //field cannot pass by matching itself
      assertThat(serviceInfoData.runtimeContext.hostname.value).isNotEmpty()
    }
  }

  @Test
  fun testRuntimeContextEndpoint() = testApplication {
    application {
      installJson()

      MetaRouting(
        service = ServiceDescriptor("test-service", apiPrefix = "test-service", Port(1234)),
        baseUrl = Url.pattern("test-service/meta"),
        authenticationRequirement = AuthenticationRequirement.None,
      ).configure(this)
    }

    val client = createClient {
      install(ContentNegotiation) {
        json()
      }
    }

    client.get("test-service/meta/runtime-context") {
      accept(ContentType.Application.Json)
    }.let { response ->
      assertThat(response).isOk()
      val runtimeContextData = response.body<RuntimeContextDetailsResponse>().dataOrNull()
      assertThat(runtimeContextData).isNotNull()

      //The projection survives the round trip as a whole...
      assertThat(runtimeContextData).isEqualTo(RuntimeContext.current.toDetails())
      //...and carries a resolved hostname - asserted on its own so a projection that dropped the
      //field cannot pass by matching itself
      assertThat(runtimeContextData!!.hostname.value).isNotEmpty()
    }
  }

  @Test
  fun `the available feature flags answer under forceServerError`() = testApplication {
    application {
      installJson()
      install(FeatureFlagsPlugin)

      MetaRouting(
        service = ServiceDescriptor("test-service", apiPrefix = "test-service", Port(1234)),
        baseUrl = Url.pattern("test-service/meta"),
        authenticationRequirement = AuthenticationRequirement.None,
      ).configure(this)
    }

    val response = client.get("test-service/meta/feature-flags/available") {
      header(FeatureFlags.HeaderKey, "forceServerError")
    }

    assertThat(response).isOk()
  }
}
