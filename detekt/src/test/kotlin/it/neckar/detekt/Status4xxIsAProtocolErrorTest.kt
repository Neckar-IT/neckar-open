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
import assertk.assertions.isEqualTo
import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class Status4xxIsAProtocolErrorTest {

  private val rule = Status4xxIsAProtocolError(Config.empty)

  @Test
  fun `flags a named client-error constant`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.BadRequest)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("HttpStatusCode.BadRequest")
    assertThat(findings[0].message).contains("carries `@Suppress(\"Status4xxIsAProtocolError\")`")
  }

  @Test
  fun `flags the lowest and the highest client-error code`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.fromValue(400))
        call.respond(HttpStatusCode(499, "Client Closed Request"))
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `ignores a status the source compares against`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      fun isClientError(status: HttpStatusCode): Boolean {
        return status >= HttpStatusCode.BadRequest && status != HttpStatusCode.Conflict
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a status an identity comparison, a bracket, a further selector and a when subject test against`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      fun describe(status: HttpStatusCode): String {
        if (status === ((HttpStatusCode.Gone))) return "gone"
        if (status == HttpStatusCode(428, "Precondition Required")) return "precondition"
        if (status == io.ktor.http.HttpStatusCode(429, "Too Many Requests")) return "throttled"
        if (status.value == HttpStatusCode.Unauthorized.value) return "unauthorized"
        return when (status) {
          HttpStatusCode.Conflict -> "conflict"
          else -> "other"
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a status a further selector reads outside a condition`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.Conflict.value)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a status the source names with backticks`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.`fromValue`(409))
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a status behind an unaliased import of the companion`() {
    val findings = rule.lint(
      """
      import io.ktor.http.HttpStatusCode.Companion
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(Companion.Locked)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags fromValue with a client-error code`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.fromValue(409))
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("HttpStatusCode.fromValue(409)")
  }

  @Test
  fun `flags the constructor with a client-error code`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode(428, "Precondition Required"))
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("HttpStatusCode(428")
  }

  @Test
  fun `flags the constructor whose named arguments put the description first`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode(description = "Precondition Required", value = 428))
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a fully qualified client-error constant`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(io.ktor.http.HttpStatusCode.Gone)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags the import of a single client-error constant`() {
    val findings = rule.lint(
      """
      import io.ktor.http.HttpStatusCode.Conflict
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(Conflict)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].entity.ktElement.text).isEqualTo("import io.ktor.http.HttpStatusCode.Conflict")
    assertThat(findings[0].message).contains("names the status through `HttpStatusCode`")
  }

  @Test
  fun `ignores the import of a client-error constant in a file answering no request`() {
    val findings = rule.lint(
      """
      import io.ktor.client.statement.HttpResponse
      import io.ktor.http.HttpStatusCode.Conflict

      fun judge(response: HttpResponse): Boolean {
        return response.status == Conflict
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a status the source reaches through the companion`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.Companion.Conflict)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores the import of a constant outside the client-error range`() {
    val findings = rule.lint(
      """
      import io.ktor.http.HttpStatusCode.OK
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(OK)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags the import alone where a local declaration shadows the constant`() {
    val findings = rule.lint(
      """
      import io.ktor.http.HttpStatusCode.Conflict
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        val Conflict = "the local one"
        call.respondText(Conflict)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].entity.ktElement.text).isEqualTo("import io.ktor.http.HttpStatusCode.Conflict")
  }

  @Test
  fun `flags a status behind an import alias`() {
    val findings = rule.lint(
      """
      import io.ktor.http.HttpStatusCode as Status
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(Status.Gone)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Status.Gone")
  }

  @Test
  fun `flags the lookup and the constructor behind an import alias`() {
    val findings = rule.lint(
      """
      import io.ktor.http.HttpStatusCode as Status
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(Status.fromValue(409))
        call.respond(Status(428, "Precondition Required"))
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `flags a status behind an alias of the companion`() {
    val findings = rule.lint(
      """
      import io.ktor.http.HttpStatusCode.Companion as Codes
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(Codes.Gone)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags both spellings a file gives the status class`() {
    val findings = rule.lint(
      """
      import io.ktor.http.HttpStatusCode
      import io.ktor.http.HttpStatusCode as Status
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.Gone)
        call.respond(Status.Conflict)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }


  @Test
  fun `flags every status of a file once`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.BadRequest)
        call.respond(HttpStatusCode.Conflict)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `flags a status in a file reaching the Ktor server through a wildcard import`() {
    val findings = rule.lint(
      """
      import io.ktor.server.response.*

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.Forbidden)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags a status in a file importing both the Ktor client and the Ktor server`() {
    val findings = rule.lint(
      """
      import io.ktor.client.statement.HttpResponse
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.Locked)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `reports the status itself, not the call around it`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.Conflict)
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].entity.ktElement.text).isEqualTo("HttpStatusCode.Conflict")
  }

  @Test
  fun `ignores a status a variable carries into the call`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any, code: Int) {
        call.respond(HttpStatusCode.fromValue(code))
        call.respond(HttpStatusCode(code, "Unknown"))
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }


  @Test
  fun `ignores a success status`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.OK)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a server-error status`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.InternalServerError)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores fromValue and the constructor outside the client-error range`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.fromValue(503))
        call.respond(HttpStatusCode(302, "Found"))
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a file importing the Ktor client alone`() {
    val findings = rule.lint(
      """
      import io.ktor.client.statement.HttpResponse

      suspend fun retry(response: HttpResponse, fallback: (HttpStatusCode) -> Unit) {
        fallback(HttpStatusCode.NotFound)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores a declaration carrying the suppression`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      // The route answers a missing credential.
      @Suppress("Status4xxIsAProtocolError")
      suspend fun route(call: Any) {
        call.respond(HttpStatusCode.Unauthorized)
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  /**
   * `PsiTreeUtil.getParentOfType(element, KtAnnotated)` reaches the `KtAnnotatedExpression` around a
   * statement, so the suppression holds one statement of a lambda and leaves its siblings reported.
   */
  @Test
  fun `the suppression holds a single annotated statement`() {
    val findings = rule.lint(
      """
      import io.ktor.server.routing.get

      suspend fun route(call: Any) {
        handle {
          // The client sent no credential.
          @Suppress("Status4xxIsAProtocolError")
          call.respond(HttpStatusCode.Unauthorized)
          call.respond(HttpStatusCode.Conflict)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("HttpStatusCode.Conflict")
  }

}
