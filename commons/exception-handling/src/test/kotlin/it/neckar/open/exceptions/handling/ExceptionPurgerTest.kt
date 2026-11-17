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
package it.neckar.open.exceptions.handling

import it.neckar.open.exceptions.handling.ExceptionPurger.purge
import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Assertions

import org.junit.jupiter.api.Test
import java.io.IOException

/**
 */
class ExceptionPurgerTest {
  @Test
  fun purgeTest() {
    val found = purge(RuntimeException(IOException()))
    assertThat(found).isInstanceOf(IOException::class.java)
  }

  @Test
  fun `getRoot returns the deepest cause`() {
    val root = IOException("root")
    val middle = RuntimeException("middle", root)
    val outer = RuntimeException("outer", middle)

    assertThat(ExceptionPurger.getRoot(outer)).isSameInstanceAs(root)
  }

  @Test
  fun `getRoot returns the exception itself when there is no cause`() {
    val e = IllegalStateException("alone")
    assertThat(ExceptionPurger.getRoot(e)).isSameInstanceAs(e)
  }

  @Test
  fun `getLocalizedRootMessage returns the message of the root`() {
    val root = IOException("root")
    val outer = RuntimeException("outer", root)

    val message: String? = ExceptionPurger.getLocalizedRootMessage(outer)
    assertThat(message).isEqualTo("root")
  }

  @Test
  fun `getLocalizedRootMessage returns null when the root has no message`() {
    //A throwable without a message is the common case - a NullPointerException thrown by the JVM, for example
    val message: String? = ExceptionPurger.getLocalizedRootMessage(RuntimeException("outer", IllegalStateException()))
    assertThat(message).isNull()
  }

  @Test
  fun `getRoot terminates on a self-referential cause`() {
    // Throwable.initCause forbids self-references at the JDK level,
    // so subclass to expose a cause-cycle without going through initCause.
    class CyclicException : RuntimeException("loop") {
      override val cause: Throwable get() = this
    }
    val e = CyclicException()

    // Before the fix, this call hung the test thread.
    val root = ExceptionPurger.getRoot(e)
    assertThat(root).isSameInstanceAs(e)
  }
}
