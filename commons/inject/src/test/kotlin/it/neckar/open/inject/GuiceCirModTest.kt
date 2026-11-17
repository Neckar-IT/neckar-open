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

import it.neckar.open.inject.GuiceModulesHelper.minimize
import com.google.inject.AbstractModule
import com.google.inject.BindingAnnotation
import com.google.inject.Guice
import com.google.inject.Provides
import jakarta.inject.Inject
import jakarta.inject.Qualifier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Arrays

/**
 *
 */
class GuiceCirModTest {
  @Test
  fun testIt() {
    val myObject = Guice.createInjector(Module1(), Module2()).getInstance(MyObject::class.java)
    assertEquals("", Guice.createInjector(Module1(), Module2()).getInstance(String::class.java))
    assertEquals(7, myObject.id.toLong())
    val result = minimize(listOf(Module1(), Module2()), MyObject::class.java)
    assertEquals(0, result.getRemoved().size.toLong())
  }

  class Module1 : AbstractModule() {
    override fun configure() {
      // No bindings — uses @Provides methods only
    }

    @Provides
    fun providesInteger(@StringMarker string: String?): Int {
      assertEquals("magic", string)
      return 7
    }
  }

  class Module2 : AbstractModule() {
    override fun configure() {
      bind(String::class.java).annotatedWith(StringMarker::class.java).toInstance("magic")
    }
  }

  class MyObject @Inject constructor(val id: Int)

  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
  @MustBeDocumented
  @Qualifier
  @BindingAnnotation
  annotation class StringMarker
}
