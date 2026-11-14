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

class SerializableMustUseNativeUuidTest {

  private val rule = SerializableMustUseNativeUuid(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags constructor parameter with raw Uuid`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import kotlin.uuid.Uuid
      import kotlinx.serialization.Serializable

      @Serializable
      data class BookEntity(val id: Uuid, val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("id")
  }

  @Test
  fun `flags body property with raw Uuid`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import kotlin.uuid.Uuid
      import kotlinx.serialization.Serializable

      @Serializable
      class BookEntity(val title: String) {
        var ownerId: Uuid? = null
      }
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("ownerId")
  }

  @Test
  fun `does not flag NativeUuid property`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import it.neckar.uuid.NativeUuid
      import kotlinx.serialization.Serializable

      @Serializable
      data class BookEntity(val id: NativeUuid, val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
      NATIVE_UUID_STUB,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag Uuid in non-Serializable class`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlin.uuid.Uuid

      data class Book(val id: Uuid, val title: String)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags multiple Uuid properties`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import kotlin.uuid.Uuid
      import kotlinx.serialization.Serializable

      @Serializable
      data class BookEntity(
        val id: Uuid,
        val ownerId: Uuid,
        val title: String,
      )
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).hasSize(2)
  }

  companion object {
    val NATIVE_UUID_STUB: String = """
      package it.neckar.uuid

      import kotlin.uuid.Uuid

      typealias NativeUuid = Uuid
    """.trimIndent()
  }
}
