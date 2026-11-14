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

class RestResponseConstructorMustUseFactoryTest {

  private val rule = RestResponseConstructorMustUseFactory(Config.empty)
  private val environment = createEnvironment()

  /**
   * Stub for `it.neckar.rest.RestResponse` and a `kotlinx.serialization.Serializable` annotation
   * stub. Both are inlined as a dependency source so the test fixtures can be self-contained
   * without bringing the real REST module onto the test classpath.
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

  /**
   * The "well-formed" sealed parent file. Every test fixture co-locates its sealed parent and
   * leaf subclasses in a single file (mirroring the real-world `…Response.kt` files in
   * `internal/patterns/.../messages/`). Construction sites in *this* file are whitelisted by the
   * rule's "same KtFile" exemption.
   */
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
  fun `flags direct constructor of leaf subclass at external call site`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.usage
      import example.responses.MyResponse

      fun produce(): MyResponse = MyResponse.Good("payload")
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      responseDefinition,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Good")
    assertThat(findings[0].message).contains("MyResponse")
  }

  @Test
  fun `flags data object reference at external call site`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.usage
      import example.responses.MyResponse

      fun produce(): MyResponse.AlreadyArchived = MyResponse.AlreadyArchived
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      responseDefinition,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("AlreadyArchived")
  }

  @Test
  fun `accepts construction inside the sealed parent's file`() {
    val findings = rule.lintWithContext(
      environment,
      """
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

      fun helperInSameFile(data: String): MyResponse = MyResponse.Good(data)
      """.trimIndent(),
      restResponseStub,
      serializableStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts call to companion factory at external call site`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.usage
      import example.responses.MyResponse

      fun produce(): MyResponse = MyResponse.good("payload")
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      responseDefinition,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts non-sealed Serializable data class construction`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.usage
      import it.neckar.rest.RestResponse
      import kotlinx.serialization.Serializable

      @Serializable data class CreateAuthorResponse(val id: String) : RestResponse<String>

      fun produce(): CreateAuthorResponse = CreateAuthorResponse("a1")
      """.trimIndent(),
      restResponseStub,
      serializableStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores sealed Serializable hierarchy that is not a RestResponse`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.usage
      import kotlinx.serialization.Serializable

      @Serializable
      sealed interface UnrelatedShape {
        @Serializable data class Circle(val radius: Double) : UnrelatedShape
      }

      fun produce(): UnrelatedShape = UnrelatedShape.Circle(1.0)
      """.trimIndent(),
      serializableStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores data object reference used as when-condition pattern`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.usage
      import example.responses.MyResponse

      fun handle(response: MyResponse) {
        when (response) {
          is MyResponse.Good -> println(response.data)
          MyResponse.AlreadyArchived -> println("archived")
        }
      }
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      responseDefinition,
    )

    // Exhaustive `when` matching against a `data object` singleton is an equality check, not a
    // construction site. No leaf-typed value flows out, so the rule must stay silent.
    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores type references in declarations`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package example.usage
      import example.responses.MyResponse

      fun extractGood(response: MyResponse): MyResponse.Good? = response as? MyResponse.Good
      """.trimIndent(),
      restResponseStub,
      serializableStub,
      responseDefinition,
    )

    // `as? MyResponse.Good` is a type cast, not a leaf-typed value construction. The
    // smart-cast result expression is left to RespondWithSealedSubclass at the respond site.
    assertThat(findings).isEmpty()
  }
}
