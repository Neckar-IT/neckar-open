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

/**
 * The name is a question, not an answer: it asks for the marker instead of standing in for one.
 */
class WireNameRequiresMarkerTest {

  private val rule = WireNameRequiresMarker(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags a serializable view name without the annotation`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlinx.serialization.Serializable

      @Serializable
      data class BookDetails(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("BookDetails")
  }

  @Test
  fun `flags a transport word without a marker`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlinx.serialization.Serializable

      @Serializable
      data class BookBorrowedPayload(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `accepts the annotation`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.open.annotations.Messaging
      import kotlinx.serialization.Serializable

      @Messaging
      @Serializable
      data class BookBorrowedEvent(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
      DomainMustNotBeSerializableTest.MessagingStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts the DataView annotation`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.open.annotations.DataView
      import kotlinx.serialization.Serializable

      @DataView("the book as a table row")
      @Serializable
      data class BookDetails(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
      DomainMustNotBeSerializableTest.DataViewStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a wire package`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.rest.views

      import kotlinx.serialization.Serializable

      @Serializable
      data class BookDetails(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `leaves a class alone that goes nowhere`() {
    // `MouseEvent` in `it.neckar.events` is an in-memory input object. A transport word is only a
    // promise when something is actually serialized.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.input

      data class MouseEvent(val x: Int, val y: Int)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `leaves a plain domain name alone`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlinx.serialization.Serializable

      @Serializable
      data class Book(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `carries the question to the leaves of a hierarchy`() {
    // The leaf inherits the promise from the name above it, the same way it would inherit a marker.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlinx.serialization.Serializable

      @Serializable
      sealed interface LibraryMessage {
        @Serializable
        data class Borrowed(val title: String) : LibraryMessage
      }
      """.trimIndent(),
      DomainMustNotBeSerializableTest.SerializableStub,
    )

    assertThat(findings).hasSize(2)
  }
}
