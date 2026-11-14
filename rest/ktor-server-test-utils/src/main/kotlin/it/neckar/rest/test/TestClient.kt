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
package it.neckar.rest.test

import assertk.*
import assertk.assertions.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.testing.*
import io.ktor.util.reflect.*
import it.neckar.ktor.client.request
import it.neckar.ktor.jwt.JwtConfiguration
import it.neckar.open.http.Url
import it.neckar.open.kotlin.lang.findSealedInterface
import it.neckar.open.tenancy.TenantDescriptor
import it.neckar.rest.NoContentResponse
import it.neckar.rest.RestResponse
import it.neckar.user.UserLoginName
import kotlinx.serialization.SerializationException

/**
 * Test client for services
 */
class TestClient(val httpClient: HttpClient) {
  suspend inline fun <reified ResponseType : RestResponse<*>> get(
    url: Url.Absolute,
    contentType: ContentType = ContentType.Application.Json,
    /**
     * The expected status code of the response.
     * If null, the status code is not checked.
     */
    expectedStatus: HttpStatusCode? = null,
    noinline block: HttpRequestBuilder.() -> Unit = {},
  ): Assert<ResponseType> {
    return sendRequestAndCheckResponse(
      url = url, method = HttpMethod.Get, body = null, expectedStatus = expectedStatus, contentType = contentType, block = block
    )
  }

  suspend inline fun <reified ResponseType : RestResponse<*>> delete(
    url: Url.Absolute,
    contentType: ContentType = ContentType.Application.Json,
    /**
     * The expected status code of the response.
     * If null, the status code is not checked.
     */
    expectedStatus: HttpStatusCode? = null,
    noinline block: HttpRequestBuilder.() -> Unit = {},
  ): Assert<ResponseType> {
    return sendRequestAndCheckResponse(
      url = url, method = HttpMethod.Delete, body = null, expectedStatus = expectedStatus, contentType = contentType, block = block
    )
  }

  suspend inline fun <reified ResponseType : RestResponse<*>> put(
    url: Url.Absolute,
    body: Any?,
    contentType: ContentType = ContentType.Application.Json,
    /**
     * The expected status code of the response.
     * If null, the status code is not checked.
     */
    expectedStatus: HttpStatusCode? = null,
    /**
     * The type the body is serialized as. Required for a generic body — see
     * [sendRequestAndCheckResponse].
     */
    bodyTypeInfo: TypeInfo? = null,
    noinline block: HttpRequestBuilder.() -> Unit = {},
  ): Assert<ResponseType> {
    return sendRequestAndCheckResponse(
      url = url, method = HttpMethod.Put, body = body, expectedStatus = expectedStatus, contentType = contentType,
      bodyTypeInfo = bodyTypeInfo, block = block
    )
  }

  suspend inline fun <reified ResponseType : RestResponse<*>> post(
    url: Url.Absolute,
    body: Any?,
    contentType: ContentType = ContentType.Application.Json,
    /**
     * The expected status code of the response.
     * If null, the status code is not checked.
     */
    expectedStatus: HttpStatusCode? = null,
    /**
     * The type the body is serialized as. Required for a generic body — see
     * [sendRequestAndCheckResponse].
     */
    bodyTypeInfo: TypeInfo? = null,
    noinline block: HttpRequestBuilder.() -> Unit = {},
  ): Assert<ResponseType> {
    return sendRequestAndCheckResponse(
      url = url, method = HttpMethod.Post, body = body, expectedStatus = expectedStatus, contentType = contentType,
      bodyTypeInfo = bodyTypeInfo, block = block
    )
  }

  /**
   * Sends a request and checks the response.
   */
  suspend inline fun <reified ResponseType : RestResponse<*>> sendRequestAndCheckResponse(
    url: Url.Absolute,
    method: HttpMethod,
    body: Any? = null,
    contentType: ContentType = ContentType.Application.Json,
    /**
     * The expected status code of the response.
     * If null, the status code is not checked.
     */
    expectedStatus: HttpStatusCode? = null,
    bodyTypeInfo: TypeInfo? = null,
    noinline block: HttpRequestBuilder.() -> Unit = {},
  ): Assert<ResponseType> {

    return sendRequestAndCheckResponse(
      url = url,
      method = method,
      body = body,
      contentType = contentType,
      expectedStatus = expectedStatus,
      responseTypeInfo = typeInfo<ResponseType>(),
      bodyTypeInfo = bodyTypeInfo,
      block = block
    )
  }

  suspend fun <ResponseType : RestResponse<*>> sendRequestAndCheckResponse(
    url: Url.Absolute,
    method: HttpMethod,
    body: Any? = null,
    contentType: ContentType = ContentType.Application.Json,
    /**
     * The expected status code of the response.
     * If null, the status code is not checked.
     */
    expectedStatus: HttpStatusCode? = null,
    responseTypeInfo: TypeInfo,
    /**
     * The type [body] is serialized as. `null` derives it from the body's runtime class, which
     * carries no type arguments — a generic body type has to state it, e.g.
     * `typeInfo<UpsertArticleConditionsTimelineEntryRequest<BareArticleConditions>>()`.
     */
    bodyTypeInfo: TypeInfo? = null,
    block: HttpRequestBuilder.() -> Unit = {},
  ): Assert<ResponseType> {

    require(RestResponse::class.java.isAssignableFrom(responseTypeInfo.type.java)) {
      "responseType must extend RestResponse, but was: ${responseTypeInfo.type.qualifiedName}"
    }

    val response = httpClient.request(url) {
      this.method = method
      accept(contentType)
      if (body != null) {
        contentType(ContentType.Application.Json)
        setBody(body, bodyTypeInfo ?: body.erasedTypeInfo())
      }
      block()
    }

    if (expectedStatus != null) {
      assertThat(response.status).isEqualTo(expectedStatus)
    }

    if (response.status == HttpStatusCode.NoContent) {
      val bodyAsText = response.bodyAsText()
      assertThat(bodyAsText).isEmpty()
      assertThat(responseTypeInfo.type).isEqualTo(NoContentResponse::class)
      return assertThat(NoContentResponse) as Assert<ResponseType> //Return this assertion to allow chaining
    }

    assertThat(response.status.isSuccess(), response.status.toString()).isTrue()
    try {
      return assertThat(response.body<ResponseType>(responseTypeInfo))
    } catch (e: SerializationException) {
      throw AssertionError("Failed to deserialize response body to [${responseTypeInfo.type.qualifiedName}]: ${e.message}\nResponse body text:\n${response.bodyAsText()}", e)
    } catch (e: ContentConvertException) {
      throw AssertionError("Failed to deserialize response body to [${responseTypeInfo.type.qualifiedName}]: ${e.message}\nResponse body text:\n${response.bodyAsText()}", e)
    }
  }
}

/**
 * Creates a new test client.
 * Calls [setupTestClient] to configure the http client that can be used to test services.
 */
fun ApplicationTestBuilder.createTestClient(
  /**
   * The optional JWT configuration to use.
   * If provided, the client will be configured to use JWT authentication.
   */
  jwtConfiguration: JwtConfiguration? = null,
  /**
   * The tenant the client authenticates as.
   * Only relevant if [jwtConfiguration] is provided.
   */
  tenantDescriptor: TenantDescriptor = TestTenantDescriptor,
  /**
   * The user the client authenticates as. Capability checks resolve it against the service's user
   * store, so pass the login name of a provisioned user when a route demands a capability.
   * Only relevant if [jwtConfiguration] is provided.
   */
  loginName: UserLoginName = UserLoginName("test"),
  /**
   * Applied to the client configuration after the defaults of [setupTestClient] — use it to
   * install additional plugins or override defaults.
   */
  additionalClientConfig: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit = {},
): TestClient {
  return TestClient(
    createClient {
      setupTestClient(jwtConfiguration = jwtConfiguration, tenantDescriptor = tenantDescriptor, loginName = loginName)
      additionalClientConfig()
    }
  )
}

/**
 * The type info a body is serialized as when the caller states none: the body's own class, or the
 * sealed interface it belongs to so the polymorphic discriminator is written.
 *
 * Carries no type arguments. A body whose class has type parameters cannot be serialized from it —
 * the caller states the type itself through `bodyTypeInfo`.
 */
private fun Any.erasedTypeInfo(): TypeInfo {
  val bodyClass = this::class

  require(bodyClass.typeParameters.isEmpty()) {
    "Body of type <$bodyClass> has type parameters — pass bodyTypeInfo = typeInfo<...>() with the type arguments filled in"
  }

  val sealedInterface = bodyClass.findSealedInterface()
  return when (sealedInterface) {
    null -> TypeInfo(bodyClass, bodyClass.java, null)
    else -> TypeInfo(sealedInterface, sealedInterface.java, null)
  }
}
