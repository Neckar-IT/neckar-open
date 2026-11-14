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
package it.neckar.open.kotlin.reflect

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.requireNotNull
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class FqClassNameTest {
  @Test
  fun `fqNameToClassForName converts top-level class names correctly`() {
    assertThat(fqNameToClassForName("com.example.MyClass")).isEqualTo("com.example.MyClass")
  }

  @Test
  fun `fqNameToClassForName converts inner class names correctly`() {
    assertThat(fqNameToClassForName("com.example.OuterClass.InnerClass")).isEqualTo("com.example.OuterClass\$InnerClass")
  }

  @Test
  fun testIt() {
    verifyFqName(kClass = FqClassNameTest::class, kotlinFqName = "it.neckar.open.kotlin.reflect.FqClassNameTest", javaFqName = $$"it.neckar.open.kotlin.reflect.FqClassNameTest")
    verifyFqName(kClass = Foo::class, kotlinFqName = "it.neckar.open.kotlin.reflect.FqClassNameTest.Foo", javaFqName = $$"it.neckar.open.kotlin.reflect.FqClassNameTest$Foo")
    verifyFqName(kClass = Foo.Bar::class, kotlinFqName = "it.neckar.open.kotlin.reflect.FqClassNameTest.Foo.Bar", javaFqName = $$"it.neckar.open.kotlin.reflect.FqClassNameTest$Foo$Bar")
    verifyFqName(kClass = Foo.Bar.FooBar::class, kotlinFqName = "it.neckar.open.kotlin.reflect.FqClassNameTest.Foo.Bar.FooBar", javaFqName = $$"it.neckar.open.kotlin.reflect.FqClassNameTest$Foo$Bar$FooBar")
  }

  private fun verifyFqName(kClass: KClass<*>, kotlinFqName: String, javaFqName: String) {
    val fqName = kClass.qualifiedName.requireNotNull()

    assertThat(fqName).isEqualTo(kotlinFqName)
    assertThat(kClass.java.name).isEqualTo(javaFqName)

    val converted = fqNameToClassForName(fqName)
    assertThat(converted).isEqualTo(javaFqName)

    assertThat(Class.forName(converted)).isNotNull()
  }

  class Foo {
    class Bar {
      class FooBar {
      }
    }
  }
}
