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
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.assertions.single
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Location
import dev.detekt.api.SourceLocation
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test

class MissingClientSessionRuleTest {

  private val rule = MissingClientSessionRule(Config.empty)
  private val environment = createEnvironment()

  @Test
  fun `flags a driver call without the session of the ActiveTransaction in scope`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.MongoCollection
      import it.neckar.open.mongodb.ActiveTransaction

      class BookAccess(val books: MongoCollection<String>) {
        context(tx: ActiveTransaction)
        fun countBooks(): Long = books.countDocuments()
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).single().prop(Finding::message).contains("collection.countDocuments(tx.clientSession, …)")
  }

  @Test
  fun `accepts a driver call with the session of the ActiveTransaction in scope`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.MongoCollection
      import it.neckar.open.mongodb.ActiveTransaction

      class BookAccess(val books: MongoCollection<String>) {
        context(tx: ActiveTransaction)
        fun countBooks(): Long = books.countDocuments(tx.clientSession)
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).isEmpty()
  }

  @Test
  fun `inside a transaction block accepts the session of the block and flags a call without it`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.MongoCollection
      import it.neckar.open.mongodb.ActiveTransaction
      import it.neckar.open.mongodb.withTransactionRetrying

      suspend fun countBooks(books: MongoCollection<String>): Long = withTransactionRetrying {
        val inTransaction = books.countDocuments(contextOf<ActiveTransaction>().clientSession)
        inTransaction + books.countDocuments()
      }
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).single().prop(Finding::entity).prop(Entity::location).prop(Location::source).prop(SourceLocation::line).isEqualTo(9)
  }

  @Test
  fun `accepts a migration that passes its ClientSession`() {
    val findings = rule.lintWithContext(
      environment,
      """
      package it.neckar.example

      import com.mongodb.kotlin.client.coroutine.ClientSession
      import com.mongodb.kotlin.client.coroutine.MongoCollection

      context(clientSession: ClientSession)
      fun countBooks(books: MongoCollection<String>): Long = books.countDocuments(clientSession)
      """.trimIndent(),
      *Stubs,
    )

    assertThat(findings).isEmpty()
  }

  private companion object {
    private val Stubs: Array<String> = arrayOf(
      """
      package com.mongodb.kotlin.client.coroutine

      class ClientSession

      class MongoCollection<T : Any> {
        fun countDocuments(): Long = 0
        fun countDocuments(clientSession: ClientSession): Long = 0
      }
      """.trimIndent(),
      """
      package it.neckar.open.mongodb

      import com.mongodb.kotlin.client.coroutine.ClientSession

      sealed interface ActiveTransaction {
        val clientSession: ClientSession
      }

      suspend fun <T> withTransactionRetrying(block: suspend context(ActiveTransaction) () -> T): T = TODO()
      """.trimIndent(),
    )
  }
}
