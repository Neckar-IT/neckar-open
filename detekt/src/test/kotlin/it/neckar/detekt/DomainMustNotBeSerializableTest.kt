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

class DomainMustNotBeSerializableTest {

  private val rule = DomainMustNotBeSerializable(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags @Serializable domain class`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlinx.serialization.Serializable

      @Serializable
      data class Book(val title: String)
      """.trimIndent(),
      SerializableStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Book")
  }

  @Test
  fun `ignores domain class without @Serializable`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      data class Book(val title: String)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores @Serializable Mongo entity`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import it.neckar.open.mongodb.dao.MongoDocument
      import kotlinx.serialization.Serializable

      @Serializable
      data class BookEntity(val title: String) : MongoDocument
      """.trimIndent(),
      SerializableStub,
      MongoDocumentStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores @Serializable REST DataView class with suffix`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.rest.views

      import kotlinx.serialization.Serializable

      @Serializable
      data class BookDetails(val title: String)
      """.trimIndent(),
      SerializableStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores @Serializable @DataView-annotated class without suffix`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.rest

      import it.neckar.open.annotations.DataView
      import kotlinx.serialization.Serializable

      @DataView("book table row")
      @Serializable
      data class BookRow(val title: String)
      """.trimIndent(),
      SerializableStub,
      DataViewStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores @Serializable @CrossLayer-annotated domain class`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.open.annotations.CrossLayer
      import kotlinx.serialization.Serializable

      @CrossLayer("Money is shared by Domain+REST")
      @Serializable
      data class Money(val cents: Long)
      """.trimIndent(),
      SerializableStub,
      CrossLayerStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores @Serializable value class`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlinx.serialization.Serializable

      @JvmInline
      @Serializable
      value class Isbn(val raw: String)
      """.trimIndent(),
      SerializableStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores @Serializable enum`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlinx.serialization.Serializable

      @Serializable
      enum class BookGenre { Fiction, NonFiction }
      """.trimIndent(),
      SerializableStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores domain class outside it_neckar package`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package com.example.book

      import kotlinx.serialization.Serializable

      @Serializable
      data class Book(val title: String)
      """.trimIndent(),
      SerializableStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a domain class named like an event`() {
    // "Event" is a business word too — a domain event is a domain concept, and `MouseEvent` in
    // `it.neckar.events` is an in-memory object. Only the package exempts a whole protocol.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlinx.serialization.Serializable

      @Serializable
      data class BookBorrowedEvent(val title: String)
      """.trimIndent(),
      SerializableStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("BookBorrowedEvent")
  }

  @Test
  fun `ignores an annotated message`() {
    // A message crosses a boundary just like a REST DTO does, only over a different transport.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.open.annotations.Messaging
      import kotlinx.serialization.Serializable

      @Messaging
      @Serializable
      data class BookBorrowed(val title: String)
      """.trimIndent(),
      SerializableStub,
      MessagingStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores the leaves of an annotated hierarchy`() {
    // Annotating every leaf would only repeat the envelope, so the enclosing parent counts.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.open.annotations.Messaging
      import kotlinx.serialization.Serializable

      @Messaging
      @Serializable
      sealed interface LibraryMessage {
        @Serializable
        data class Borrowed(val title: String) : LibraryMessage
      }
      """.trimIndent(),
      SerializableStub,
      MessagingStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a message that only looks like one`() {
    // The name promises a transport; without the marker the class stays Domain and gets checked.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import kotlinx.serialization.Serializable

      @Serializable
      data class BookBorrowedPayload(val title: String)
      """.trimIndent(),
      SerializableStub,
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `ignores a class in a messaging package`() {
    // The package says it: whatever lives here describes what goes over the wire.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.messaging

      import kotlinx.serialization.Serializable

      @Serializable
      data class Borrowed(val title: String)
      """.trimIndent(),
      SerializableStub,
    )

    assertThat(findings).isEmpty()
  }

  companion object {
    val MessagingStub: String = """
      package it.neckar.open.annotations

      @Target(AnnotationTarget.CLASS)
      annotation class Messaging
    """.trimIndent()

    val SerializableStub: String = """
      package kotlinx.serialization

      @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
      annotation class Serializable
    """.trimIndent()

    val MongoDocumentStub: String = """
      package it.neckar.open.mongodb.dao

      interface MongoDocument
    """.trimIndent()

    val DataViewStub: String = """
      package it.neckar.open.annotations

      @Target(AnnotationTarget.CLASS)
      annotation class DataView(val useCaseDescription: String)
    """.trimIndent()

    val ScriptBoundaryStub: String = """
      package it.neckar.open.annotations

      @Target(AnnotationTarget.CLASS)
      annotation class ScriptBoundary
    """.trimIndent()

    val CrossLayerStub: String = """
      package it.neckar.open.annotations

      @Target(AnnotationTarget.CLASS)
      annotation class CrossLayer(val reason: String)
    """.trimIndent()
  }
}
