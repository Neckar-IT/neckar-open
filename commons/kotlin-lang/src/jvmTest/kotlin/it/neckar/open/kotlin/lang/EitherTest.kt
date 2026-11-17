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
package it.neckar.open.kotlin.lang

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test
import kotlin.concurrent.atomics.AtomicBoolean

class EitherTest {
  @Test
  fun `init fails with descriptive message when both values are null`() {
    assertFailure {
      Either<String, String>(null, null)
    }.hasMessage("Either must contain one value, but does not contain any")
  }

  @Test
  fun `init fails with descriptive message when both values are non-null`() {
    assertFailure {
      Either("a", "b")
    }.hasMessage("Either must contain one value, but contains two")
  }

  @Test
  fun testEitherOnFirst() {
    val called = AtomicBoolean(false)

    assertThat(Either("a", null).onFirst {
      called.store(true)
    })

    assertThat(called.load()).isTrue()
  }

  @Test
  fun testEitherOnSecond() {
    val called = AtomicBoolean(false)

    assertThat(Either(null, "b").onSecond {
      called.store(true)
    })

    assertThat(called.load()).isTrue()
  }

  @Test
  fun testMapFirst() {
    Either(null, "b").mapOnFirst {
      fail("Should not be called")
    }.let {
      assertThat(it).isNull()
    }

    Either(17, null).mapOnFirst {
      it.toString()
    }.let {
      assertThat(it).isEqualTo("17")
    }
  }

  @Test
  fun testMapSecond() {
    Either("a", null).mapOnSecond {
      fail("Should not be called")
    }.let {
      assertThat(it).isNull()
    }

    Either(null, 17).mapOnSecond {
      it.toString()
    }.let {
      assertThat(it).isEqualTo("17")
    }
  }
}
