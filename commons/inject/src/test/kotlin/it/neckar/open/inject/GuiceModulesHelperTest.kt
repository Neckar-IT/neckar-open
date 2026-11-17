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

import com.google.inject.AbstractModule
import com.google.inject.ConfigurationException
import com.google.inject.CreationException
import com.google.inject.Key
import it.neckar.open.inject.GuiceModulesHelper.assertMinimizeNotPossible
import it.neckar.open.inject.GuiceModulesHelper.minimize
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Arrays
import kotlin.test.assertFailsWith

/**
 *
 */
class GuiceModulesHelperTest {
  @Test
  fun testFail() {
    assertFailsWith<ConfigurationException> {
      minimize(Arrays.asList(Module1(), Module2()), MutableList::class.java)
    }
  }

  @Test
  fun testMinimization() {
    val result = minimize(Arrays.asList(Module1(), Module2()), Any::class.java)
    assertEquals(2, result.getRemoved().size)
    assertEquals(0, result.getTypes().size)
  }

  @Test
  fun testMinimization2() {
    val result = minimize(Arrays.asList(Module1(), Module2()), MyObject::class.java)
    assertEquals(1, result.getRemoved().size)
    assertEquals(1, result.getTypes().size)
  }

  @Test
  fun testMinimization3() {
    val result = GuiceModulesHelper.Result(Arrays.asList(Module1(), Module2()))
    Assertions.assertSame(result, minimize(result, MyObject::class.java))
    assertEquals(1, result.getRemoved().size)
    assertEquals(1, result.getTypes().size)
  }

  @Test
  @Throws(Exception::class)
  fun testAssertMini() {
    assertThrows(AssertionError::class.java) { assertMinimizeNotPossible(Arrays.asList(Module1(), Module2()), MyObject::class.java) }
  }

  @Test
  @Throws(Exception::class)
  fun testAssertMini4() {
    assertThrows(AssertionError::class.java) { assertMinimizeNotPossible(listOf(Module2()), Any::class.java) }
  }

  @Test
  @Throws(Exception::class)
  fun testAssertMini2() {
    assertMinimizeNotPossible(listOf(Module1()), MyObject::class.java, Any::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun testAssertMini3() {
    //    GuiceModulesHelper.assertMinimizeNotPossible( Arrays.asList( new Module1() ), MyObject.class, Object.class );
    assertMinimizeNotPossible(listOf(Module1()), MyObject::class.java)
    assertMinimizeNotPossible(listOf(Module1()), Any::class.java, MyObject::class.java)
    assertMinimizeNotPossible(listOf(Module1()), object : Key<MyObject?>() {}, object : Key<Any?>() {})
    assertMinimizeNotPossible(listOf(Module1()), object : Key<Any?>() {}, object : Key<MyObject?>() {}, object : Key<Any?>() {})
  }

  @Test
  fun `minimize without keys fails instead of reporting that nothing can be removed`() {
    val result = GuiceModulesHelper.Result(listOf(Module1(), Module2()))

    val thrown = assertFailsWith<IllegalArgumentException> {
      minimize(result)
    }
    assertEquals("Need at least one key", thrown.message)
  }

  @Test
  fun `a module that fails to configure surfaces its own exception instead of counting as removable`() {
    val result = GuiceModulesHelper.Result(listOf(Module1(), BrokenModule()))

    val thrown = assertFailsWith<CreationException> {
      minimize(result, Key.get(MyObject::class.java))
    }
    assertEquals("BrokenModule is broken", thrown.errorMessages.single().cause?.message)
  }

  class Module1 : AbstractModule() {
    override fun configure() {
      bind(MyObject::class.java).toInstance(MyObject("theId"))
    }
  }

  class Module2 : AbstractModule() {
    override fun configure() {
      // Intentionally empty — used as empty-module placeholder in minimization tests
    }
  }

  /**
   * A module that is broken itself - as opposed to a module that is merely superfluous
   */
  class BrokenModule : AbstractModule() {
    override fun configure() {
      throw IllegalStateException("BrokenModule is broken")
    }
  }

  class MyObject(private val id: String)
}
