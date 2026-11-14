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

class RespondWithSealedSubclassTest {

  private val rule = RespondWithSealedSubclass(Config.empty)
  private val environment = createEnvironment()

  /**
   * Stub for `it.neckar.rest.RestResponse`, the kotlinx-serialization annotations, and the Ktor
   * `ApplicationCall.respond` extension functions. Inlined as dependency sources so the test
   * fixtures don't need the real Ktor / REST modules on the classpath.
   *
   * The package and function names match the real Ktor types (`io.ktor.server.response.respond`,
   * `…respondNullable`) — the rule resolves callees by FQN, so the names matter.
   */
  private val restResponseStub = """
    package it.neckar.rest
    interface RestResponse<out DataType>
  """.trimIndent()

  private val serializableStub = """
    package kotlinx.serialization
    @Target(AnnotationTarget.CLASS) annotation class Serializable
    @Target(AnnotationTarget.CLASS) annotation class SerialName(val value: String)
  """.trimIndent()

  private val ktorStub = """
    package io.ktor.server.application
    interface ApplicationCall
  """.trimIndent()

  private val ktorResponseStub = """
    package io.ktor.server.response
    import io.ktor.server.application.ApplicationCall
    fun ApplicationCall.respond(message: Any) {}
    fun ApplicationCall.respondNullable(message: Any?) {}
  """.trimIndent()

  private val responseDefinition = """
    package example.responses
    import it.neckar.rest.RestResponse
    import kotlinx.serialization.Serializable

    @Serializable
    sealed interface MyResponse : RestResponse<String> {
      @Serializable data class Good(val data: String) : MyResponse
      @Serializable data object AlreadyArchived : MyResponse

      companion object {
        fun good(data: String): MyResponse = Good(data)
        fun alreadyArchived(): MyResponse = AlreadyArchived
      }
    }
  """.trimIndent()

  @Test
  fun `flags respond with direct subclass constructor argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.MyResponse
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respond

      fun handle(call: ApplicationCall) {
        call.respond(MyResponse.Good("payload"))
      }
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      ktorStub,
      ktorResponseStub,
      responseDefinition,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Good")
    assertThat(findings[0].message).contains("MyResponse")
    assertThat(findings[0].message).contains("respond")
  }

  @Test
  fun `flags respond with data object subclass argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.MyResponse
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respond

      fun handle(call: ApplicationCall) {
        call.respond(MyResponse.AlreadyArchived)
      }
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      ktorStub,
      ktorResponseStub,
      responseDefinition,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("AlreadyArchived")
  }

  @Test
  fun `flags respondNullable with subclass argument`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.MyResponse
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respondNullable

      fun handle(call: ApplicationCall) {
        call.respondNullable(MyResponse.Good("payload"))
      }
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      ktorStub,
      ktorResponseStub,
      responseDefinition,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("respondNullable")
  }

  @Test
  fun `accepts respond with sealed-parent typed variable`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.MyResponse
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respond

      fun handle(call: ApplicationCall) {
        val response: MyResponse = MyResponse.Good("payload")
        call.respond(response)
      }
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      ktorStub,
      ktorResponseStub,
      responseDefinition,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts respond with companion factory call`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.MyResponse
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respond

      fun handle(call: ApplicationCall) {
        call.respond(MyResponse.good("payload"))
      }
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      ktorStub,
      ktorResponseStub,
      responseDefinition,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts respond with non-sealed Serializable data class`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import it.neckar.rest.RestResponse
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respond
      import kotlinx.serialization.Serializable

      @Serializable data class CreateAuthorResponse(val id: String) : RestResponse<String>

      fun handle(call: ApplicationCall) {
        call.respond(CreateAuthorResponse("a1"))
      }
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      ktorStub,
      ktorResponseStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores sealed Serializable hierarchy that is not a RestResponse`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import io.ktor.server.application.ApplicationCall
      import io.ktor.server.response.respond
      import kotlinx.serialization.Serializable

      @Serializable
      sealed interface UnrelatedShape {
        @Serializable data class Circle(val radius: Double) : UnrelatedShape
      }

      fun handle(call: ApplicationCall) {
        call.respond(UnrelatedShape.Circle(1.0))
      }
      """.trimIndent(),
      serializableStub,
      ktorStub,
      ktorResponseStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores user-defined respond function on a different receiver`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.routing
      import example.responses.MyResponse

      class CustomReceiver {
        fun respond(message: Any) {}
      }

      fun handle() {
        val custom = CustomReceiver()
        custom.respond(MyResponse.Good("payload"))
      }
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      responseDefinition,
    )

    assertThat(findings).isEmpty()
  }
}
