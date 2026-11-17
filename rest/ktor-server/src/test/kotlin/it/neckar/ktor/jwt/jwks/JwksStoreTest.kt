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
package it.neckar.ktor.jwt.jwks

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import it.neckar.open.time.now
import org.junit.jupiter.api.Test

class JwksStoreTest {
  private val seedKey = JwksTestKey.generate("seed-key")
  private val fetchedKey = JwksTestKey.generate("fetched-key")
  private val persistedKey = JwksTestKey.generate("persisted-key")

  @Test
  fun `holds nothing until something is adopted`() {
    val store = JwksStore()

    assertThat(store.held).isNull()
    assertThat(store.keyOf(seedKey.keyId)).isNull()
  }

  @Test
  fun `serves from the seed while nothing better is known`() {
    val store = JwksStore()

    assertThat(store.adoptSeed(jwksDocumentOf(seedKey))).isTrue()
    assertThat(store.held?.origin).isEqualTo(JwksOrigin.Seed)
    assertThat(store.keyOf(seedKey.keyId)).isNotNull()
  }

  @Test
  fun `a successful fetch replaces the seed and the seed is never taken again`() {
    val store = JwksStore()
    assertThat(store.adoptSeed(jwksDocumentOf(seedKey))).isTrue()

    store.adoptFetched(jwksDocumentOf(fetchedKey))

    assertThat(store.held?.origin).isEqualTo(JwksOrigin.Live)
    assertThat(store.keyOf(seedKey.keyId)).isNull()
    assertThat(store.keyOf(fetchedKey.keyId)).isNotNull()

    assertThat(store.adoptSeed(jwksDocumentOf(seedKey))).isFalse()
    assertThat(store.keyOf(fetchedKey.keyId)).isNotNull()
  }

  @Test
  fun `the persisted set of an earlier process counts as confirmed, so the seed stays unused`() {
    val store = JwksStore()

    assertThat(store.adoptPersisted(jwksDocumentOf(persistedKey), now())).isTrue()
    assertThat(store.held?.origin).isEqualTo(JwksOrigin.Persisted)

    assertThat(store.adoptSeed(jwksDocumentOf(seedKey))).isFalse()
    assertThat(store.keyOf(persistedKey.keyId)).isNotNull()
  }

  @Test
  fun `the persisted set never overwrites one that is already held`() {
    val store = JwksStore()
    store.adoptFetched(jwksDocumentOf(fetchedKey))

    assertThat(store.adoptPersisted(jwksDocumentOf(persistedKey), now())).isFalse()
    assertThat(store.held?.origin).isEqualTo(JwksOrigin.Live)
  }

  @Test
  fun `a fetch replaces the held set whole, it never merges`() {
    val store = JwksStore()
    store.adoptFetched(jwksDocumentOf(fetchedKey, seedKey))

    store.adoptFetched(jwksDocumentOf(fetchedKey))

    assertThat(store.keyOf(seedKey.keyId)).isNull()
  }

  @Test
  fun `the persisted set keeps the fetch time, not the read time`() {
    val store = JwksStore()
    val fetchedAt = now() - kotlin.time.Duration.parse("3h")

    assertThat(store.adoptPersisted(jwksDocumentOf(persistedKey), fetchedAt)).isTrue()

    assertThat(store.held?.obtainedAt).isEqualTo(fetchedAt)
  }
}
