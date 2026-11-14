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

class DomainMustNotReferenceDataViewTest {

  private val rule = DomainMustNotReferenceDataView(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags domain class with REST suffix property`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.rest.views.BookDetails

      data class Book(val title: String, val details: BookDetails)
      """.trimIndent(),
      """
        package it.neckar.example.book.rest.views
        data class BookDetails(val title: String)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("BookDetails")
  }

  @Test
  fun `flags domain class with @DataView-annotated reference`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.rest.BookRow

      data class Book(val title: String, val row: BookRow)
      """.trimIndent(),
      """
        package it.neckar.example.book.rest

        import it.neckar.open.annotations.DataView

        @DataView("table row")
        data class BookRow(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.DataViewStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("BookRow")
  }

  @Test
  fun `does not flag domain class with another domain reference`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.author.Author

      data class Book(val title: String, val author: Author)
      """.trimIndent(),
      """
        package it.neckar.example.author
        data class Author(val name: String)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag a messaging type referencing a DataView`() {
    // An SSE event carries the same view both frontends already read off the stream.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.messaging

      import it.neckar.example.book.rest.views.BookDetails

      sealed interface LibraryEvent {
        data class Borrowed(val book: BookDetails) : LibraryEvent
      }
      """.trimIndent(),
      """
        package it.neckar.example.book.rest.views
        data class BookDetails(val title: String)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag a client referencing a DataView`() {
    // A client for a foreign service is infrastructure and owns that service's wire types.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.rest.views.BookDetails

      class LibraryClient {
        fun fetch(): BookDetails = BookDetails("")
      }
      """.trimIndent(),
      """
        package it.neckar.example.book.rest.views
        data class BookDetails(val title: String)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag a @ScriptBoundary class referencing a DataView`() {
    // A resolver that builds a pricing script's input lives in the domain package next to the domain classes.
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.pricing

      import it.neckar.example.book.rest.views.BookPricingContext
      import it.neckar.open.annotations.ScriptBoundary

      @ScriptBoundary
      class BookPricingContextResolver {
        fun resolve(): BookPricingContext = BookPricingContext("")
      }
      """.trimIndent(),
      """
        package it.neckar.example.book.rest.views

        import it.neckar.open.annotations.DataView

        @DataView("pricing script input")
        data class BookPricingContext(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.DataViewStub,
      DomainMustNotBeSerializableTest.ScriptBoundaryStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags the same class without @ScriptBoundary`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.pricing

      import it.neckar.example.book.rest.views.BookPricingContext

      class BookPricingContextResolver {
        fun resolve(): BookPricingContext = BookPricingContext("")
      }
      """.trimIndent(),
      """
        package it.neckar.example.book.rest.views

        import it.neckar.open.annotations.DataView

        @DataView("pricing script input")
        data class BookPricingContext(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.DataViewStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("BookPricingContext")
  }

  @Test
  fun `does not flag top-level extension function`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.rest.views.BookDetails

      data class Book(val title: String)

      fun Book.toDetails(): BookDetails = BookDetails(title)
      """.trimIndent(),
      """
        package it.neckar.example.book.rest.views
        data class BookDetails(val title: String)
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
