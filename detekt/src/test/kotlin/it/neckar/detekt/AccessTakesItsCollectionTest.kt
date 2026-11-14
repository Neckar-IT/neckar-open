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
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test

class AccessTakesItsCollectionTest {

  private val rule = AccessTakesItsCollection(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags an Access that takes a MongoConnection, a subclass of it or MongoCollections`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.ManagedMongoDatabase
      import it.neckar.open.mongodb.MongoCollections
      import it.neckar.open.mongodb.MongoDatabaseConnection

      class ShopStore : ManagedMongoDatabase()

      class BookAccess(private val database: MongoDatabaseConnection)
      class AuthorAccess(private val store: ShopStore)
      class PublisherAccessAllTenants(private val collections: MongoCollections)
      class ReviewAccess(private val collections: MongoCollections?)
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings.map { it.message.substringBefore(" takes") }).containsExactlyInAnyOrder("BookAccess", "AuthorAccess", "PublisherAccessAllTenants", "ReviewAccess")
    assertThat(findings[0].message).contains("CollectionPerTenant")
  }

  @Test
  fun `flags the written type name of a type that does not resolve`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class BookAccess(private val database: MongoDatabaseConnection)
      class AuthorAccess(private val collections: MongoCollections?)
      class PublisherAccess(private val database: ServiceDatabase)
      class EditionAccess(private val database: MongoConnection)
      abstract class ReviewAccess : TransactionScope
      class OrderAccess(private val database: MongoDatabase)
      """.trimIndent(),
    )

    assertThat(findings.map { it.message.substringBefore(" ") }).containsExactlyInAnyOrder("BookAccess", "AuthorAccess", "PublisherAccess", "EditionAccess", "ReviewAccess")
  }

  @Test
  fun `flags an Access that implements TransactionScope`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.TransactionScope

      abstract class BookAccess : TransactionScope
      abstract class AuthorAccessAllTenants : TransactionScope
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(2)
    assertThat(findings[0].message).contains("implements TransactionScope")
  }

  @Test
  fun `accepts an Access on its collections and a Support that takes the database handle`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.MongoCollection
      import it.neckar.open.mongodb.CollectionPerTenant
      import it.neckar.open.mongodb.MongoCollections
      import it.neckar.open.mongodb.MongoDatabaseConnection
      import it.neckar.open.mongodb.TransactionScope

      class BookAccess(private val bookCollection: CollectionPerTenant<BookEntity>, private val authorAccess: AuthorAccess) {
        companion object {
          fun of(collections: MongoCollections, authorAccess: AuthorAccess): BookAccess = BookAccess(CollectionPerTenant(), authorAccess)
        }
      }
      class AuthorAccess(private val authorCollection: MongoCollection<AuthorEntity>)
      class BookSupport(private val bookAccess: BookAccess, private val transactionScope: TransactionScope)
      class MigrationRunner(private val database: MongoDatabaseConnection)
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts a resolved type named like a database handle that does not extend MongoConnection`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class ReadOnlyMongoDatabase

      class BookAccess(private val snapshot: ReadOnlyMongoDatabase)
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags by the written name a handle whose base class does not resolve`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      class ShopDatabase : UnknownBase()

      class BookAccess(private val database: ShopDatabase)
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings.map { it.message.substringBefore(" takes") }).containsExactlyInAnyOrder("BookAccess")
  }

  @Test
  fun `flags a handle that resolves although another supertype does not`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.ManagedMongoDatabase

      class ShopStore : ManagedMongoDatabase(), UnknownInterface

      class BookAccess(private val store: ShopStore)
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings.map { it.message.substringBefore(" takes") }).containsExactlyInAnyOrder("BookAccess")
  }

  @Test
  fun `flags a type parameter bounded by MongoConnection`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.MongoConnection

      class BookAccess<D : MongoConnection>(private val database: D)
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings.map { it.message.substringBefore(" takes") }).containsExactlyInAnyOrder("BookAccess")
  }

  companion object {
    private val Stubs: Array<String> = arrayOf(
      """
      package com.mongodb.kotlin.client.coroutine

      class MongoCollection<T : Any>
      """.trimIndent(),
      """
      package it.neckar.open.mongodb

      abstract class MongoConnection
      abstract class MongoDatabaseConnection : MongoConnection()
      abstract class ManagedMongoDatabase : MongoDatabaseConnection()
      open class MongoCollections
      class CollectionPerTenant<T>
      interface TransactionScope
      """.trimIndent(),
      """
      package it.neckar.example

      class BookEntity
      class AuthorEntity
      """.trimIndent(),
    )
  }
}
