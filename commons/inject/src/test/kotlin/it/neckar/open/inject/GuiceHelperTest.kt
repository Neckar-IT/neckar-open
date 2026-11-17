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
package it.neckar.open.inject

import it.neckar.open.inject.GuiceHelper.bindWildcardCollectionForSet
import it.neckar.open.inject.GuiceHelper.superCollectionOf
import it.neckar.open.inject.GuiceHelper.superListOf
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Key
import com.google.inject.util.Types
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 *
 */
class GuiceHelperTest {
  @Test
  fun testBindCollections() {
    val module: AbstractModule = object : AbstractModule() {
      override fun configure() {
        val setKey = Key.get(Types.setOf(String::class.java)) as Key<Set<String>>
        bind(setKey).toInstance(HashSet())
        bindWildcardCollectionForSet<Any>(binder(), String::class.java)
      }
    }
    Guice.createInjector(module)
  }

  @Test
  fun testIt() {
    assertEquals("java.util.Collection<? extends java.lang.String>", superCollectionOf(String::class.java).toString())
    assertEquals("java.util.List<? extends java.lang.String>", superListOf(String::class.java).toString())
  }

  @Test
  fun testGuice() {
    assertEquals("java.util.List<java.lang.String>", Types.listOf(String::class.java).toString())
    assertEquals("java.util.List<? extends java.lang.String>", Types.listOf(Types.subtypeOf(String::class.java)).toString())
    assertEquals("java.util.Collection<? extends java.lang.String>", Types.newParameterizedType(MutableCollection::class.java, Types.subtypeOf(String::class.java)).toString())
  }
}
