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

class EntityMustNotReferenceDomainTest {

  private val rule = EntityMustNotReferenceDomain(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags entity class with domain property`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import it.neckar.example.book.Book
      import it.neckar.open.mongodb.dao.MongoDocument

      data class BookEntity(val title: String, val book: Book) : MongoDocument
      """.trimIndent(),
      """
        package it.neckar.example.book
        data class Book(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Book")
  }

  @Test
  fun `does not flag top-level toModel extension`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import it.neckar.example.book.Book
      import it.neckar.open.mongodb.dao.MongoDocument

      data class BookEntity(val title: String) : MongoDocument

      fun BookEntity.toModel(): Book = Book(title)
      fun Book.toEntity(): BookEntity = BookEntity(title)
      """.trimIndent(),
      """
        package it.neckar.example.book
        data class Book(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag entity referencing value class from domain package`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import it.neckar.example.book.BookId
      import it.neckar.open.mongodb.dao.MongoDocument

      data class BookEntity(val id: BookId, val title: String) : MongoDocument
      """.trimIndent(),
      """
        package it.neckar.example.book

        @JvmInline
        value class BookId(val raw: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).isEmpty()
  }
}
