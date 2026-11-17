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

class EntityMustNotReferenceDataViewTest {

  private val rule = EntityMustNotReferenceDataView(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags entity class with REST suffix property`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import it.neckar.example.book.rest.views.BookDetails
      import it.neckar.open.mongodb.dao.MongoDocument

      data class BookEntity(val title: String, val details: BookDetails) : MongoDocument
      """.trimIndent(),
      """
        package it.neckar.example.book.rest.views
        data class BookDetails(val title: String)
      """.trimIndent(),
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("BookDetails")
  }

  @Test
  fun `does not flag entity referencing another entity`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.book.mongo

      import it.neckar.example.author.mongo.AuthorEntity
      import it.neckar.open.mongodb.dao.MongoDocument

      data class BookEntity(val title: String, val author: AuthorEntity) : MongoDocument
      """.trimIndent(),
      """
        package it.neckar.example.author.mongo

        import it.neckar.open.mongodb.dao.MongoDocument

        data class AuthorEntity(val name: String) : MongoDocument
      """.trimIndent(),
      DomainMustNotBeSerializableTest.MongoDocumentStub,
    )

    assertThat(findings).isEmpty()
  }
}
