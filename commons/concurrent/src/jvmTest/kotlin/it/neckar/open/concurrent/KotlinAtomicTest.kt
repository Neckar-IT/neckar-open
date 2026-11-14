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
package it.neckar.open.concurrent

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicIntArray
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch

/**
 *
 */
class KotlinAtomicTest {
  @Test
  fun testAtomicBoolean() {
    val atomic = AtomicBoolean(false)
    assertThat(atomic.load()).isEqualTo(false)

    atomic.store(true)
    assertThat(atomic.load()).isEqualTo(true)
  }

  @Test
  fun testAtomicInt() {
    val atomic = AtomicInt(0)
    assertThat(atomic.load()).isEqualTo(0)

    atomic.store(1)
    assertThat(atomic.load()).isEqualTo(1)

    assertThat(atomic.incrementAndFetch()).isEqualTo(2)
    assertThat(atomic.load()).isEqualTo(2)

    assertThat(atomic.decrementAndFetch()).isEqualTo(1)
    assertThat(atomic.load()).isEqualTo(1)
  }

  @Test
  fun testAtomicIntWithInitialValue() {
    val atomic = AtomicInt(5)
    assertThat(atomic.load()).isEqualTo(5)

    atomic.store(1)
    assertThat(atomic.load()).isEqualTo(1)

    assertThat(atomic.incrementAndFetch()).isEqualTo(2)
    assertThat(atomic.load()).isEqualTo(2)

    assertThat(atomic.decrementAndFetch()).isEqualTo(1)
    assertThat(atomic.load()).isEqualTo(1)
  }

  @Test
  fun testAtomicIntWithInitialValueAndIncrement() {
    val atomic = AtomicInt(5)
    assertThat(atomic.load()).isEqualTo(5)

    assertThat(atomic.incrementAndFetch()).isEqualTo(6)
    assertThat(atomic.load()).isEqualTo(6)

    assertThat(atomic.decrementAndFetch()).isEqualTo(5)
    assertThat(atomic.load()).isEqualTo(5)
  }

  @Test
  fun testIntArray() {
    val atomic = AtomicIntArray(5)
    assertThat(atomic.size).isEqualTo(5)

    for (i in 0 until atomic.size) {
      assertThat(atomic.loadAt(i)).isEqualTo(0)
    }

    atomic.storeAt(0, 1)
    assertThat(atomic.loadAt(0)).isEqualTo(1)

    atomic.storeAt(1, 2)
    assertThat(atomic.loadAt(1)).isEqualTo(2)
  }
}
