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

class MessagingMustNotReferenceDomainTest {

  private val rule = MessagingMustNotReferenceDomain(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags an annotated message embedding a domain class`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.open.annotations.Messaging

      @Messaging
      data class BookBorrowed(val book: Book)

      data class Book(val title: String)
      """.trimIndent(),
      MessagingStub,
    )

    assertThat(findings).hasSize(1)
    // The host name contains "Book" as well, so the assertion names the referenced type explicitly.
    assertThat(findings[0].message).contains("the Domain class `Book`")
  }

  @Test
  fun `flags a class in a messaging package embedding a domain class`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.messaging

      import it.neckar.example.book.Book

      data class Borrowed(val book: Book)
      """.trimIndent(),
      """
        package it.neckar.example.book
        data class Book(val title: String)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Book")
  }

  @Test
  fun `does not flag a payload carrying a REST view`() {
    // The SSE stream of huddle does exactly this: the event carries the view both frontends read.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.messaging

      import it.neckar.example.book.rest.views.BookDetails

      data class Borrowed(val book: BookDetails)
      """.trimIndent(),
      """
        package it.neckar.example.book.rest.views
        data class BookDetails(val title: String)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag a payload carrying an id or an enum`() {
    // Value classes and enums are cross-layer by convention — an id goes into a message unchanged.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.messaging

      import it.neckar.example.book.BookId
      import it.neckar.example.book.BookState

      data class Borrowed(val id: BookId, val state: BookState)
      """.trimIndent(),
      """
        package it.neckar.example.book

        @JvmInline
        value class BookId(val value: String)

        enum class BookState { Available, Borrowed }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag a payload carrying the leaf of a CrossLayer hierarchy`() {
    // `Url` declares itself cross-layer once; `Url.Absolute` must not have to repeat it.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.messaging

      import it.neckar.open.http.Url

      data class Borrowed(val link: Url.Absolute)
      """.trimIndent(),
      """
        package it.neckar.open.http

        import it.neckar.open.annotations.CrossLayer

        @CrossLayer("URL value type")
        sealed interface Url {
          data class Absolute(val value: String) : Url
        }
      """.trimIndent(),
      DomainMustNotBeSerializableTest.CrossLayerStub,
    )

    assertThat(findings).isEmpty()
  }

  companion object {
    private val MessagingStub: String = DomainMustNotBeSerializableTest.MessagingStub
  }
}
