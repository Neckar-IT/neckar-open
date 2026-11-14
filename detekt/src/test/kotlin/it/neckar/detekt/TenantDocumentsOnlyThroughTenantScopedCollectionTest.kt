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

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test

class TenantDocumentsOnlyThroughTenantScopedCollectionTest {

  private val rule = TenantDocumentsOnlyThroughTenantScopedCollection(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags the raw collection of tenant documents`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.MongoDatabase
      import it.neckar.open.mongodb.MongoCollections

      class BookAccess(val collections: MongoCollections, val database: MongoDatabase) {
        fun booksOfEveryTenant() = collections.allTenants<BookEntity>()
        fun booksOfEveryTenantByDescriptor() = collections.allTenants(BookDescriptor)
        fun booksFromTheDriver() = database.getCollection<BookEntity>("books")
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(3)
    assertThat(findings.map { it.message }).each {
      it.all {
        contains("tenantScoped")
        contains("perTenant")
      }
    }
  }

  @Test
  fun `accepts the tenant-scoped collection and the raw collection of documents without tenant`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.MongoCollections
      import it.neckar.open.tenancy.Tenant

      class BookQueries(val collections: MongoCollections) {
        fun books(tenantId: Tenant.Id) = collections.tenantScoped<BookEntity>(tenantId)
        fun meta() = collections[MetaDescriptor]
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts an Access built on perTenant`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.CollectionPerTenant
      import it.neckar.open.mongodb.MongoCollections
      import it.neckar.open.tenancy.Tenant

      class BookAccess(val bookCollection: CollectionPerTenant<BookEntity>) {
        fun books(tenantId: Tenant.Id) = bookCollection.of(tenantId)

        companion object {
          fun of(collections: MongoCollections) = BookAccess(collections.perTenant(BookDescriptor))
        }
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `accepts the collection of every tenant in code marked AllTenants and in a migration`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.MongoCollection
      import it.neckar.open.mongodb.AllTenants
      import it.neckar.open.mongodb.MongoCollections
      import it.neckar.open.mongodb.migration.MongoDbMigrator

      @AllTenants(reason = "Login resolves the user before the tenant is known")
      class BookAccessAllTenants(val bookCollectionAllTenants: MongoCollection<BookEntity>) {
        fun countBooks() = bookCollectionAllTenants.countDocuments()

        companion object {
          fun of(collections: MongoCollections) = BookAccessAllTenants(collections.allTenants(BookDescriptor))
        }
      }

      class BookAccess(val collections: MongoCollections) {
        @AllTenants(reason = "The operator lists the books of every tenant")
        fun booksOfEveryTenant() = collections.allTenants<BookEntity>()
      }

      class BookMigration(val collections: MongoCollections) : MongoDbMigrator {
        fun books() = collections.allTenants(BookDescriptor)
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `flags a filter of tenant documents that runs without a tenant`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.AllTenants
      import it.neckar.open.mongodb.MongoDatabaseConnection
      import it.neckar.open.mongodb.MongoFilter
      import it.neckar.open.tenancy.Tenant

      class ServiceDatabase : MongoDatabaseConnection()

      class BookAccess(val database: ServiceDatabase) {
        suspend fun books(filter: MongoFilter<BookEntity>) = database.withTransaction { database.findByFilter<BookEntity>(filter) }
        suspend fun booksOf(filter: MongoFilter<BookEntity>, tenantId: Tenant.Id) = database.withTransaction { database.findByFilter(filter, tenantId) }
        suspend fun meta(filter: MongoFilter<MetaEntity>) = database.withTransaction { database.findByFilter(filter) }
      }

      @AllTenants(reason = "The operator searches the books of every tenant")
      class BookAccessAllTenants(val database: ServiceDatabase) {
        suspend fun books(filter: MongoFilter<BookEntity>) = database.withTransaction { database.findByFilter<BookEntity>(filter) }
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).all {
      contains("findByFilter")
      contains("perTenant")
    }
  }

  @Test
  fun `flags paging across tenants outside code marked AllTenants`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.AllTenants
      import it.neckar.open.mongodb.query.MongoPageQuery

      class BookAccess {
        fun books() = MongoPageQuery.forAllTenants<BookEntity>()
        fun meta() = MongoPageQuery.forAllTenants<MetaEntity>()
      }

      @AllTenants(reason = "The operator pages the books of every tenant")
      class BookAccessAllTenants {
        fun books() = MongoPageQuery.forAllTenants<BookEntity>()
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("forAllTenants")
  }

  @Test
  fun `flags a class whose name starts with Admin`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.MongoCollections

      class AdminBookAccess(val collections: MongoCollections) {
        fun booksOfEveryTenant() = collections.allTenants<BookEntity>()
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `flags every use of a collection of tenant documents that arrived as parameter or property`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.MongoCollection

      class BookAccess(private val books: MongoCollection<BookEntity>, private val meta: MongoCollection<MetaEntity>) {
        fun count() = books.countDocuments()
        fun countOf(other: MongoCollection<BookEntity>) = other.countDocuments()
        fun countMeta() = meta.countDocuments()
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(2)
    assertThat(findings.map { it.message }).each {
      it.all {
        contains("tenantScoped")
        contains("perTenant")
      }
    }
  }

  @Test
  fun `flags the index access, a callable reference and a local variable once each`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.MongoCollection
      import it.neckar.open.mongodb.MongoCollections

      class BookAccess(val collections: MongoCollections, val books: MongoCollection<BookEntity>) {
        fun byIndex() = collections[BookDescriptor]
        fun counter() = books::countDocuments
        fun countTwice(): Long {
          val local = collections.allTenants<BookEntity>()
          return local.countDocuments() + local.countDocuments()
        }
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(3)
  }

  @Test
  fun `flags an extension on MongoDatabaseConnection that runs a filter of tenant documents without a tenant`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import it.neckar.open.mongodb.MongoDatabaseConnection
      import it.neckar.open.mongodb.MongoFilter
      import it.neckar.open.tenancy.Tenant

      fun MongoDatabaseConnection.countBooks(filter: MongoFilter<BookEntity>): Int = 0
      fun MongoDatabaseConnection.countBooks(filter: MongoFilter<BookEntity>, tenantId: Tenant.Id): Int = 0

      class BookAccess(val database: MongoDatabaseConnection) {
        fun count(filter: MongoFilter<BookEntity>) = database.countBooks(filter)
        fun countOf(filter: MongoFilter<BookEntity>, tenantId: Tenant.Id) = database.countBooks(filter, tenantId)
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).contains("countBooks")
  }

  @Test
  fun `flags the raw collection read by the name of a tenant descriptor`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.MongoDatabase
      import org.bson.BsonDocument

      class BookMigrationSupport(val database: MongoDatabase) {
        fun books() = database.getCollection<BsonDocument>(BookDescriptor.collectionName)
        fun meta() = database.getCollection<BsonDocument>(MetaDescriptor.collectionName)
        fun probes() = database.getCollection<BsonDocument>("probes")
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(1)
    assertThat(findings[0].message).all {
      contains("getCollection")
      contains("perTenant")
    }
  }

  @Test
  fun `flags a test that reads every tenant unless the test is marked AllTenants`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example.test

      import it.neckar.example.BookEntity
      import it.neckar.open.mongodb.AllTenants
      import it.neckar.open.mongodb.MongoCollections

      class BookAccessTest(val collections: MongoCollections) {
        fun rawBooks() = collections.allTenants<BookEntity>()
      }

      @AllTenants(reason = "Proves that the documents of two tenants stay apart")
      class TenantSeparationTest(val collections: MongoCollections) {
        fun rawBooks() = collections.allTenants<BookEntity>()
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).hasSize(1)
  }

  @Test
  fun `accepts the mongodb library itself`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.open.mongodb.valuetimeline

      import it.neckar.example.BookEntity
      import it.neckar.open.mongodb.MongoCollections

      class TimelineSupport(val collections: MongoCollections) {
        fun books() = collections.allTenants<BookEntity>()
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).isEmpty()
  }

  companion object {
    private val Stubs: Array<String> = arrayOf(
      """
      package com.mongodb.kotlin.client.coroutine

      class MongoCollection<T : Any> {
        fun countDocuments(): Long = 0
      }

      class MongoDatabase {
        inline fun <reified T : Any> getCollection(collectionName: String): MongoCollection<T> = MongoCollection()
      }
      """.trimIndent(),
      """
      package org.bson

      class BsonDocument
      """.trimIndent(),
      """
      package it.neckar.open.tenancy

      interface BelongsToTenant

      class Tenant {
        class Id
      }
      """.trimIndent(),
      """
      package it.neckar.open.mongodb.migration

      interface MongoDbMigrator
      """.trimIndent(),
      """
      package it.neckar.open.mongodb

      import com.mongodb.kotlin.client.coroutine.MongoCollection
      import it.neckar.open.tenancy.BelongsToTenant
      import it.neckar.open.tenancy.Tenant

      class MongoCollectionDescriptor<T : Any>(val collectionName: String)

      annotation class AllTenants(val reason: String)

      class TenantScopedCollection<T : BelongsToTenant>

      interface MongoFilter<in T>

      sealed interface ActiveTransaction

      open class MongoDatabaseConnection {
        suspend fun <R> withTransaction(block: suspend context(ActiveTransaction) () -> R): R = TODO()

        context(tx: ActiveTransaction)
        inline fun <reified T : Any> findByFilter(filter: MongoFilter<T>): List<T> = emptyList()
        context(tx: ActiveTransaction)
        fun <T : BelongsToTenant> findByFilter(filter: MongoFilter<T>, tenantId: Tenant.Id): List<T> = emptyList()
      }

      class MongoCollections {
        operator fun <T : Any> get(descriptor: MongoCollectionDescriptor<T>): MongoCollection<T> = MongoCollection()
        inline fun <reified T : BelongsToTenant> allTenants(): MongoCollection<T> = MongoCollection()
        fun <T : BelongsToTenant> allTenants(descriptor: MongoCollectionDescriptor<T>): MongoCollection<T> = MongoCollection()
        inline fun <reified T : BelongsToTenant> tenantScoped(tenantId: Tenant.Id): TenantScopedCollection<T> = TenantScopedCollection()
        fun <T : BelongsToTenant> perTenant(descriptor: MongoCollectionDescriptor<T>): CollectionPerTenant<T> = CollectionPerTenant()
      }

      class CollectionPerTenant<T : BelongsToTenant> {
        fun of(tenantId: Tenant.Id): TenantScopedCollection<T> = TenantScopedCollection()
      }
      """.trimIndent(),
      """
      package it.neckar.open.mongodb.query

      class MongoPageQuery<T> {
        companion object {
          fun <T : Any> forAllTenants(): MongoPageQuery<T> = MongoPageQuery()
        }
      }
      """.trimIndent(),
      """
      package it.neckar.example

      import it.neckar.open.mongodb.MongoCollectionDescriptor
      import it.neckar.open.tenancy.BelongsToTenant

      class BookEntity : BelongsToTenant

      class MetaEntity

      val BookDescriptor: MongoCollectionDescriptor<BookEntity> = MongoCollectionDescriptor("books")

      val MetaDescriptor: MongoCollectionDescriptor<MetaEntity> = MongoCollectionDescriptor("meta")
      """.trimIndent(),
    )
  }
}
