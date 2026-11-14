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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.open.javafx.properties

import assertk.*
import assertk.assertions.*
import it.neckar.open.javafx.consumeImmediately
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.System.gc
import java.util.concurrent.atomic.AtomicInteger

/**
 */
class WeakListenersTest {
  var foo: Foo = Foo()

  @BeforeEach
  internal fun setUp() {
    foo = Foo()
  }

  @Test
  internal fun testConsume() {
    assertThat(foo.name).isEqualTo("")

    val counter = AtomicInteger()

    assertThat(counter.get()).isEqualTo(0)
    registerListener(counter)

    assertThat(counter.get()).isEqualTo(1)
    foo.name = "asdf17"
    assertThat(counter.get()).isEqualTo(2)

    for (i in 1..10) {
      gc()
    }

    foo.name = "asdf2"
    assertThat(counter.get()).isEqualTo(3)
  }

  private fun registerListener(counter: AtomicInteger) {
    foo.nameProperty.consumeImmediately {
      counter.incrementAndGet()
    }
  }
}
