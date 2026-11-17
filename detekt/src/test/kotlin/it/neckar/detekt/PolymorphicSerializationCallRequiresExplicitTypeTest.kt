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
package it.neckar.detekt

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test

class PolymorphicSerializationCallRequiresExplicitTypeTest {

  private val rule = PolymorphicSerializationCallRequiresExplicitType(Config.empty)
  private val environment = createEnvironment()

  private val ktorServerStub = """
    package io.ktor.server.application
    interface ApplicationCall
  """.trimIndent()

  private val ktorResponseStub = """
    package io.ktor.server.response
    import io.ktor.server.application.ApplicationCall
    fun <T : Any> ApplicationCall.respond(message: T) {}
    fun <T> ApplicationCall.respondNullable(message: T) {}
  """.trimIndent()

  private val ktorRequestStub = """
    package io.ktor.server.request
    import io.ktor.server.application.ApplicationCall
    fun <T : Any> ApplicationCall.receive(): T = TODO()
    fun <T> ApplicationCall.receiveNullable(): T = TODO()
  """.trimIndent()

  private val ktorClientStub = """
    package io.ktor.client.request
    class HttpRequestBuilder
    fun <T> HttpRequestBuilder.setBody(value: T) {}
  """.trimIndent()

  private val ktorClientCallStub = """
    package io.ktor.client.call
    class HttpClientCall
    suspend fun <T> HttpClientCall.body(): T = TODO()
  """.trimIndent()

  private val kotlinxSerializationStub = """
    package kotlinx.serialization
    fun <T> encodeToString(value: T): String = TODO()
    fun <T> decodeFromString(string: String): T = TODO()
  """.trimIndent()

  private val kotlinxJsonStub = """
    package kotlinx.serialization.json
    interface JsonElement
    fun <T> encodeToJsonElement(value: T): JsonElement = TODO()
    fun <T> decodeFromJsonElement(element: JsonElement): T = TODO()
  """.trimIndent()

  private val responseStub = """
    package example.responses
    class UpsertResponse(val msg: String) {
      companion object {
        fun good(msg: String): UpsertResponse = UpsertResponse(msg)
      }
    }
    class UpsertRequest(val msg: String)
  """.trimIndent()

  @Test
  fun `flags respond without type argument when argument is a typed variable`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.UpsertResponse
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respond

      fun handle(call: ApplicationCall) {
        val response: UpsertResponse = UpsertResponse.good("payload")
        call.respond(response)
      }
      """.trimIndent(),
      ktorServerStub,
      ktorResponseStub,
      responseStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("respond")
    assertThat(findings[0].message).contains("<T>")
  }

  @Test
  fun `accepts respond with explicit type argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.UpsertResponse
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respond

      fun handle(call: ApplicationCall) {
        call.respond<UpsertResponse>(UpsertResponse.good("payload"))
      }
      """.trimIndent(),
      ktorServerStub,
      ktorResponseStub,
      responseStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags respondNullable without type argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.UpsertResponse
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respondNullable

      fun handle(call: ApplicationCall) {
        call.respondNullable(UpsertResponse.good("payload"))
      }
      """.trimIndent(),
      ktorServerStub,
      ktorResponseStub,
      responseStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("respondNullable")
  }

  @Test
  fun `flags receive without type argument when inferred from val type`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.UpsertRequest
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.request.receive

      fun handle(call: ApplicationCall) {
        val request: UpsertRequest = call.receive()
      }
      """.trimIndent(),
      ktorServerStub,
      ktorRequestStub,
      responseStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("receive")
  }

  @Test
  fun `accepts receive with explicit type argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.UpsertRequest
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.request.receive

      fun handle(call: ApplicationCall) {
        val request = call.receive<UpsertRequest>()
      }
      """.trimIndent(),
      ktorServerStub,
      ktorRequestStub,
      responseStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags setBody without type argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.client
      import example.responses.UpsertRequest
      import io.ktor.client.request.HttpRequestBuilder
      import io.ktor.client.request.setBody

      fun build(builder: HttpRequestBuilder, request: UpsertRequest) {
        builder.setBody(request)
      }
      """.trimIndent(),
      ktorClientStub,
      responseStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("setBody")
  }

  @Test
  fun `accepts setBody with explicit type argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.client
      import example.responses.UpsertRequest
      import io.ktor.client.request.HttpRequestBuilder
      import io.ktor.client.request.setBody

      fun build(builder: HttpRequestBuilder, request: UpsertRequest) {
        builder.setBody<UpsertRequest>(request)
      }
      """.trimIndent(),
      ktorClientStub,
      responseStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags client body() without type argument when inferred from val type`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.client
      import example.responses.UpsertResponse
      import io.ktor.client.call.HttpClientCall
      import io.ktor.client.call.body

      suspend fun handle(call: HttpClientCall) {
        val response: UpsertResponse = call.body()
      }
      """.trimIndent(),
      ktorClientCallStub,
      responseStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("body")
  }

  @Test
  fun `flags encodeToString without type argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.serialization
      import example.responses.UpsertResponse
      import kotlinx.serialization.encodeToString

      fun encode(response: UpsertResponse): String {
        return encodeToString(response)
      }
      """.trimIndent(),
      kotlinxSerializationStub,
      responseStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("encodeToString")
  }

  @Test
  fun `accepts encodeToString with explicit type argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.serialization
      import example.responses.UpsertResponse
      import kotlinx.serialization.encodeToString

      fun encode(response: UpsertResponse): String {
        return encodeToString<UpsertResponse>(response)
      }
      """.trimIndent(),
      kotlinxSerializationStub,
      responseStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags decodeFromString without type argument when inferred from val type`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.serialization
      import example.responses.UpsertResponse
      import kotlinx.serialization.decodeFromString

      fun decode(json: String) {
        val response: UpsertResponse = decodeFromString(json)
      }
      """.trimIndent(),
      kotlinxSerializationStub,
      responseStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("decodeFromString")
  }

  @Test
  fun `flags encodeToJsonElement without type argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.serialization
      import example.responses.UpsertResponse
      import kotlinx.serialization.json.encodeToJsonElement
      import kotlinx.serialization.json.JsonElement

      fun encode(response: UpsertResponse): JsonElement {
        return encodeToJsonElement(response)
      }
      """.trimIndent(),
      kotlinxJsonStub,
      responseStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("encodeToJsonElement")
  }

  @Test
  fun `ignores user-defined function with the same name on a different receiver`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.UpsertResponse

      class CustomReceiver {
        fun respond(message: Any) {}
      }

      fun handle() {
        val custom = CustomReceiver()
        custom.respond(UpsertResponse.good("payload"))
      }
      """.trimIndent(),
      responseStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores unrelated reified function not in watchlist`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.UpsertResponse

      inline fun <reified T : Any> doSomething(value: T) {}

      fun handle() {
        doSomething(UpsertResponse.good("payload"))
      }
      """.trimIndent(),
      responseStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores respondText`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respondText

      fun handle(call: ApplicationCall) {
        call.respondText("plain")
      }
      """.trimIndent(),
      ktorServerStub,
      """
      package io.ktor.server.response
      import io.ktor.server.application.ApplicationCall
      fun ApplicationCall.respondText(text: String) {}
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
