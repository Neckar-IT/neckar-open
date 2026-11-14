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

class DomainMustNotReferenceEntityTest {

  private val rule = DomainMustNotReferenceEntity(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags domain class with Mongo entity property`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.mongo.BookEntity

      data class Book(val title: String, val entity: BookEntity)
      """.trimIndent(),
      ENTITY_STUB,
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Book")
    assertThat(findings[0].message).contains("BookEntity")
  }

  @Test
  fun `flags domain class with Mongo entity in generic`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.mongo.BookEntity

      data class Book(val title: String, val drafts: List<BookEntity>)
      """.trimIndent(),
      ENTITY_STUB,
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    // PSI visits the inner BookEntity type ref; the outer List<BookEntity> ref is for `List`
    // which is not an entity, so no double flag.
    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("BookEntity")
  }

  @Test
  fun `does not flag domain referencing another domain class`() {
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
  fun `does not flag top-level extension function`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.mongo.BookEntity

      data class Book(val title: String)

      fun BookEntity.toModel(): Book = Book("x")
      fun Book.toEntity(): BookEntity = BookEntity("x")
      """.trimIndent(),
      ENTITY_STUB,
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag bootstrap class referencing Mongo entity`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.bootstrap

      import it.neckar.example.book.mongo.BookEntity

      class BookBootstrap {
        fun build(): BookEntity = BookEntity("seed")
      }
      """.trimIndent(),
      ENTITY_STUB,
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag seed step referencing Mongo entity`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.mongo.BookEntity

      class Templates<T>(val items: List<T>)

      class BookSeed(private val target: Templates<BookEntity>)

      class BranchCustomDataSeeder {
        val entity: BookEntity = BookEntity("seed")
      }
      """.trimIndent(),
      ENTITY_STUB,
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag result type nested in a support class referencing Mongo entity`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.mongo.BookEntity

      class BookSupport {
        sealed interface StoreResult {
          data class Stored(val book: BookEntity) : StoreResult
        }
      }
      """.trimIndent(),
      ENTITY_STUB,
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags type nested in a domain class referencing Mongo entity`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.mongo.BookEntity

      class Book(val title: String) {
        data class Snapshot(val entity: BookEntity)
      }
      """.trimIndent(),
      ENTITY_STUB,
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("Snapshot")
  }

  @Test
  fun `does not flag wiring class referencing Mongo entity`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.mongo.BookEntity

      class BookWiring {
        val entity: BookEntity = BookEntity("seed")
      }
      """.trimIndent(),
      ENTITY_STUB,
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `does not flag @CrossLayer-marked entity referenced from domain`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book

      import it.neckar.example.book.mongo.SharedEntity

      data class Book(val title: String, val shared: SharedEntity)
      """.trimIndent(),
      """
        package it.neckar.example.book.mongo

        import it.neckar.open.annotations.CrossLayer
        import it.neckar.open.mongodb.dao.MongoDocument

        @CrossLayer("intentionally shared")
        data class SharedEntity(val id: String) : MongoDocument
      """.trimIndent(),
      DomainMustNotBeSerializableTest.MongoDocumentStub,
      DomainMustNotBeSerializableTest.CrossLayerStub,
    )

    assertThat(findings).isEmpty()
  }

  companion object {
    val ENTITY_STUB: String = """
      package it.neckar.example.book.mongo

      import it.neckar.open.mongodb.dao.MongoDocument

      data class BookEntity(val title: String) : MongoDocument
    """.trimIndent()
  }
}
