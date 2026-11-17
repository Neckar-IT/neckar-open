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

class SerializableMustUseNativeInstantTest {

  private val rule = SerializableMustUseNativeInstant(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags constructor parameter with kotlin time Instant`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import kotlin.time.Instant
      import kotlinx.serialization.Serializable

      @Serializable
      data class BookEntity(val title: String, val createdAt: Instant)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("createdAt")
  }

  @Test
  fun `flags constructor parameter with kotlinx datetime Instant`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import kotlinx.datetime.Instant
      import kotlinx.serialization.Serializable

      @Serializable
      data class BookEntity(val title: String, val createdAt: Instant)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
      KOTLINX_DATETIME_INSTANT_STUB,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("createdAt")
  }

  @Test
  fun `flags body property with raw Instant`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import kotlin.time.Instant
      import kotlinx.serialization.Serializable

      @Serializable
      class BookEntity(val title: String) {
        var lastSeen: Instant? = null
      }
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("lastSeen")
  }

  @Test
  fun `does not flag NativeInstant property`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import it.neckar.open.kotlin.serializers.NativeInstant
      import kotlinx.serialization.Serializable

      @Serializable
      data class BookEntity(val title: String, val createdAt: NativeInstant)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
      NATIVE_INSTANT_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag Instant in non-Serializable class`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlin.time.Instant

      data class Book(val title: String, val createdAt: Instant)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags multiple Instant properties`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import kotlin.time.Instant
      import kotlinx.serialization.Serializable

      @Serializable
      data class BookEntity(
        val title: String,
        val createdAt: Instant,
        val updatedAt: Instant,
      )
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).hasSize(2)
  }

  @Test
  fun `does not flag Instant local variable inside function`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import kotlin.time.Clock
      import kotlin.time.Instant
      import kotlinx.serialization.Serializable

      @Serializable
      data class BookEntity(val title: String) {
        fun stamp() {
          val now: Instant = Clock.System.now()
          println(now)
        }
      }
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).isEmpty()
  }

  companion object {
    val KOTLINX_DATETIME_INSTANT_STUB: String = """
      package kotlinx.datetime

      class Instant
    """.trimIndent()

    val NATIVE_INSTANT_STUB: String = """
      package it.neckar.open.kotlin.serializers

      import kotlin.time.Instant

      typealias NativeInstant = Instant
    """.trimIndent()
  }
}
