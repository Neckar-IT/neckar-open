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
package it.neckar.open.test.utils

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 */
@Disabled
@ExtendWith(CatchAllExceptionsExtension::class)
class CatchAllExceptionsExtensionTest {

  @Test
  fun successfulTest() {
    val thread = Thread { throw RuntimeException("This one is ignored by JUnit!") }
    thread.start()
    thread.join()
  }

  @Test
  fun ignoredAssertion() {
    val thread = Thread {
      assertThat(true).isFalse() //will not be reported!
      throw RuntimeException("This one is ignored by JUnit!")
    }
    thread.start()
    thread.join()
  }

  @Test
  fun testIt() {
    val thread = Thread { throw IllegalStateException("Hey, are u visible?") }
    assertThat(thread.isAlive).isFalse()
    thread.start()
    thread.join()
    val thread2 = Thread { throw IllegalArgumentException("Exception2") }
    assertThat(thread2.isAlive).isFalse()
    thread2.start()
    thread2.join()
    assertThat(thread2.isAlive).isFalse()
  }

  @Test
  fun testFailing() {
    val thread = Thread { throw IllegalStateException("Hey, are u visible?") }
    assertThat(thread.isAlive).isFalse()
    thread.start()
    thread.join()
    val thread2 = Thread { throw IllegalArgumentException("Exception2") }
    assertThat(thread2.isAlive).isFalse()
    thread2.start()
    thread2.join()
    assertThat(thread2.isAlive).isFalse()
    throw UnsupportedOperationException("asdf")
  }
}
