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
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test

class AccessReturnsNoCollectionTest {

  private val rule = AccessReturnsNoCollection(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags every non-private function of a MongoDB Access that returns a collection`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class BookAccess(private val bookCollection: CollectionPerTenant<BookEntity>) {
        suspend fun getAll(tenantId: Tenant.Id): List<Book> = TODO()
        internal suspend fun getIds(tenantId: Tenant.Id): Set<Book.Id>? = TODO()
        suspend fun countPerAuthor(tenantId: Tenant.Id): Map<Author.Id, Long> = TODO()
        private suspend fun helper(): List<Book> = TODO()
      }

      class PublisherAccessAllTenants(private val publisherCollection: MongoCollection<PublisherEntity>) {
        suspend fun findAll(): Collection<Publisher> = TODO()
      }

      class IdeaAccess(private val database: IdeasDatabase) {
        suspend fun getIdeas(): List<Idea> = TODO()
      }
      """.trimIndent(),
    )

    assertThat(findings.map { it.message.substringBefore(" returns") })
      .containsExactlyInAnyOrder("BookAccess.getAll", "BookAccess.getIds", "BookAccess.countPerAuthor", "PublisherAccessAllTenants.findAll", "IdeaAccess.getIdeas")
  }

  @Test
  fun `flags a function with a context parameter that returns a collection`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class BookReviewAccess(bookReviewCollection: CollectionPerTenant<BookReviewEntity>) {
        context(tx: ActiveTransaction)
        suspend fun countActiveByBookIdPerRating(bookId: Book.Id, tenantId: Tenant.Id): List<RatingCount> = TODO()
      }
      """.trimIndent(),
    )

    assertThat(findings.map { it.message.substringBefore(" returns") }).containsExactlyInAnyOrder("BookReviewAccess.countActiveByBookIdPerRating")
  }

  @Test
  fun `accepts a flow, a page and a single result`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class BookAccess(private val bookCollection: CollectionPerTenant<BookEntity>) {
        fun queryAll(tenantId: Tenant.Id): Flow<Book> = TODO()
        suspend fun getAll(pageQuery: PageQuery, tenantId: Tenant.Id): Page<Book> = TODO()
        suspend fun find(id: Book.Id, tenantId: Tenant.Id): Book? = TODO()
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `ignores an Access of another system and a class without the Access suffix`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class GitLabThreadAccess(private val api: GitLabApi) {
        fun threads(): List<Thread> = TODO()
      }

      class BookSupport(private val bookCollection: CollectionPerTenant<BookEntity>) {
        suspend fun getAll(): List<Book> = TODO()
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags an Access that takes a database handle whose name says neither Mongo nor Database`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.ManagedMongoDatabase

      class ShopStore : ManagedMongoDatabase()

      class OrderAccess(private val store: ShopStore) {
        suspend fun getOrders(): List<Order> = TODO()
      }

      class Order
      """.trimIndent(),
      """
      package it.neckar.open.mongodb

      abstract class MongoConnection
      abstract class MongoDatabaseConnection : MongoConnection()
      abstract class ManagedMongoDatabase : MongoDatabaseConnection()
      """.trimIndent(),
    )

    assertThat(findings.map { it.message.substringBefore(" returns") }).containsExactlyInAnyOrder("OrderAccess.getOrders")
  }

  @Test
  fun `ignores an Access whose resolved parameter is named like a database handle without being one`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class ReadOnlyMongoDatabase
      class Order

      class OrderAccess(private val snapshot: ReadOnlyMongoDatabase) {
        suspend fun getOrders(): List<Order> = TODO()
      }
      """.trimIndent(),
    )

    assertThat(findings).isEmpty()
  }
}
