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
package it.neckar.open.threads

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException

/**
 *
 */
class ThreadUtilsTest {
  @Test
  fun testIt() {
    assertNotEventDispatchThread()
    Assertions.assertThrows(InvocationTargetException::class.java) { invokeInEventDispatchThread(Runnable { assertNotEventDispatchThread() }) }
  }

  @Test
  fun testEdt() {
    Assertions.assertThrows(IllegalThreadStateException::class.java) { assertEventDispatchThread() }
  }

  @Test
  fun testInvoke() {
    val called = booleanArrayOf(false)
    invokeInEventDispatchThread {
      called[0] = true
      assertThat(isEventDispatchThread()).isTrue()
      assertEventDispatchThread()
    }
    waitForEventDispatchThread()
    assertThat(called[0]).isTrue()
  }

  @Test
  fun testOther() {
    val called = booleanArrayOf(false)
    assertThat(invokeInOtherThread<Any> {
      called[0] = true
      assertThat(isEventDispatchThread()).isFalse()
      assertNotEventDispatchThread()
      "asdf"
    }).isEqualTo("asdf")
    assertThat(called[0]).isTrue()
  }

  @Test
  fun `isFxApplicationThreadName matches only the JavaFX Application Thread`() {
    assertThat(isFxApplicationThreadName("JavaFX Application Thread")).isTrue()
    assertThat(isFxApplicationThreadName("main")).isFalse()
    assertThat(isFxApplicationThreadName("JFR Periodic Tasks")).isFalse()
    assertThat(isFxApplicationThreadName(null)).isFalse()
  }

  @Test
  fun `the test thread is not the JavaFX Application Thread`() {
    assertThat(Thread.currentThread().isFxApplicationThread()).isFalse()

    val fxNamed = Thread({}, "JavaFX Application Thread")
    assertThat(fxNamed.isFxApplicationThread()).isTrue()
  }
}
