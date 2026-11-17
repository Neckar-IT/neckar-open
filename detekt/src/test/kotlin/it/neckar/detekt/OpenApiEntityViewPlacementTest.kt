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
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class OpenApiEntityViewPlacementTest {

  private val rule = OpenApiEntityViewPlacement(Config.empty)

  @Test
  fun `accepts an interface named after its entity with the companion`() {
    assertThat(
      rule.lint(
        """
        @OpenApiEntityView(Book::class)
        interface BookDataView {
          companion object {
            val entityType: EntityType = EntityType.of(Book::class)
          }
        }
        """.trimIndent(),
      )
    ).isEmpty()
  }

  @Test
  fun `flags the annotation on a data class`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Book::class)
      data class BookDetails(val title: String)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("[BookDetails] is no interface - move it to the `BookDataView` interface")
  }

  @Test
  fun `flags the annotation on an enum class`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Book::class)
      enum class BookKind { Hardcover, Paperback }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("is no interface")
  }

  @Test
  fun `flags the annotation on an object`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Book::class)
      object BookRegistry
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("is no interface")
  }

  @Test
  fun `flags the annotation on a value class`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Book::class)
      @JvmInline
      value class BookTitle(val value: String)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("is no interface")
  }

  @Test
  fun `flags a view whose name does not follow its entity`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(BookReview::class)
      interface ReviewOfABookDataView {
        companion object {
          val entityType: EntityType = EntityType.of(BookReview::class)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("its own name is [BookReviewDataView]")
  }

  @Test
  fun `flags a view without the companion value`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Author::class)
      interface AuthorDataView {
        val id: String
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("val entityType")
  }

  @Test
  fun `flags a companion naming another entity`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Author::class)
      interface AuthorDataView {
        companion object {
          val entityType: EntityType = EntityType.of(Book::class)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("as [EntityType.of(Book::class)]")
  }

  @Test
  fun `flags a companion that declares something else`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Author::class)
      interface AuthorDataView {
        companion object {
          val label: String = "author"
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("val entityType")
  }

  @Test
  fun `accepts a sealed interface view`() {
    assertThat(
      rule.lint(
        """
        @OpenApiEntityView(Project::class)
        sealed interface ProjectDataView {
          companion object {
            val entityType: EntityType = EntityType.of(Project::class)
          }
        }
        """.trimIndent(),
      )
    ).isEmpty()
  }

  @Test
  fun `flags the annotation on an unnamed companion object`() {
    val findings = rule.lint(
      """
      class Library {
        @OpenApiEntityView(Book::class)
        companion object
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("[Companion] is no interface")
  }

  @Test
  fun `flags a companion value that is no EntityType of the entity`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Author::class)
      interface AuthorDataView {
        companion object {
          val entityType = Author::class
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("as [Author::class]")
  }

  @Test
  fun `accepts a getter, a qualified class literal and a nested view`() {
    assertThat(
      rule.lint(
        """
        class Library {
          @OpenApiEntityView(it.neckar.patterns.book.Book::class)
          interface BookDataView {
            companion object Names {
              val entityType: it.neckar.rest.EntityType
                get() = it.neckar.rest.EntityType.of(it.neckar.patterns.book.Book::class)
            }
          }
        }
        """.trimIndent(),
      )
    ).isEmpty()
  }

  @Test
  fun `flags a DataView interface without the annotation`() {
    val findings = rule.lint(
      """
      interface BookDataView {
        val id: String
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("must carry `@OpenApiEntityView(Book::class)`")
  }

  @Test
  fun `ignores an interface without the annotation that is no DataView`() {
    assertThat(
      rule.lint(
        """
        interface BookLike {
          val id: String
        }
        """.trimIndent(),
      )
    ).isEmpty()
  }

  @Test
  fun `flags a companion value with another receiver, factory or arity`() {
    listOf("Other.of(Author::class)", "EntityType.from(Author::class)", "EntityType.of(Author::class, Book::class)").forEach { value ->
      val findings = rule.lint(
        """
        @OpenApiEntityView(Author::class)
        interface AuthorDataView {
          companion object {
            val entityType: EntityType = $value
          }
        }
        """.trimIndent(),
      )

      assertThat(findings).hasSize(1)
      assertThat(findings[0].message).contains("as [$value]")
    }
  }

  @Test
  fun `flags a var and a delegated entity type`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Author::class)
      interface AuthorDataView {
        companion object {
          var entityType: EntityType = EntityType.of(Author::class)
        }
      }

      @OpenApiEntityView(Book::class)
      interface BookDataView {
        companion object {
          val entityType: EntityType by lazy { EntityType.of(Book::class) }
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
    assertThat(findings[0].message).contains("[var entityType: EntityType]")
    assertThat(findings[1].message).contains("as [lazy {")
  }

  @Test
  fun `checks the companion only once the view name agrees with the annotation`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Author::class)
      interface BookDataView {
        companion object {
          val entityType: EntityType = EntityType.of(Book::class)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("its own name is [AuthorDataView]")
  }

  @Test
  fun `flags a declared type other than EntityType and a var without a type`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Author::class)
      interface AuthorDataView {
        companion object {
          val entityType: Any = EntityType.of(Author::class)
        }
      }

      @OpenApiEntityView(Book::class)
      interface BookDataView {
        companion object {
          var entityType = EntityType.of(Book::class)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(2)
    assertThat(findings[0].message).contains("[val entityType: Any]")
    assertThat(findings[1].message).contains("[var entityType]")
  }

  @Test
  fun `tells an annotated class with the view name to become an interface`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Book::class)
      data class BookDataView(val id: String)
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("make [BookDataView] an interface")
  }

  @Test
  fun `reports a wrong type once, even with a wrong value`() {
    val findings = rule.lint(
      """
      @OpenApiEntityView(Author::class)
      interface AuthorDataView {
        companion object {
          val entityType: String = "author"
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("[val entityType: String]")
  }

  @Test
  fun `reports an annotation on the companion of a view once`() {
    val findings = rule.lint(
      """
      interface BookDataView {
        @OpenApiEntityView(Book::class)
        companion object {
          val entityType: EntityType = EntityType.of(Book::class)
        }
      }
      """.trimIndent(),
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("[Companion] is no interface - move it to the `BookDataView` interface")
  }

  @Test
  fun `ignores a class named DataView without the annotation`() {
    assertThat(
      rule.lint(
        """
        data class BookDataView(val id: String)
        """.trimIndent(),
      )
    ).isEmpty()
  }
}
