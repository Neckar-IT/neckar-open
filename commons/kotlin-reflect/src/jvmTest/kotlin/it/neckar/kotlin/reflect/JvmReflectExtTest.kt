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
package it.neckar.kotlin.reflect

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class JvmReflectExtTest {
  @Test
  fun testReflectJava() {
    val testData = TestData("test", 42)

    assertThat(TestData::class.java.getDeclaredFieldValue(testData, "name")).isEqualTo("test")
    assertThat(TestData::class.java.getDeclaredFieldValue(testData, "age")).isEqualTo(42)

    TestData::class.java.setDeclaredFieldValue(testData, "name", "new-name")
    TestData::class.java.setDeclaredFieldValue(testData, "age", 43)

    assertThat(TestData::class.java.getDeclaredFieldValue(testData, "name")).isEqualTo("new-name")
    assertThat(TestData::class.java.getDeclaredFieldValue(testData, "age")).isEqualTo(43)
  }

  @Test
  fun testReflectKotlin() {
    val testData = TestData("test", 42)

    assertThat(TestData::class.java.getDeclaredFieldValue(testData, "name")).isEqualTo("test")
    assertThat(TestData::class.java.getDeclaredFieldValue(testData, "age")).isEqualTo(42)

    assertThat(testData::class.getDeclaredPropertyValue(testData, "name")).isEqualTo("test")
    assertThat(testData::class.getDeclaredPropertyValue(testData, "age")).isEqualTo(42)
  }

  @Test
  fun testSetByReflection() {
    val testData = TestData("test", 42)

    assertThat(testData::class.getDeclaredPropertyValue(testData, "name")).isEqualTo("test")
    assertThat(testData::class.getDeclaredPropertyValue(testData, "age")).isEqualTo(42)

    testData::class.setDeclaredPropertyValue(testData, "name", "new-name")
    testData::class.setDeclaredPropertyValue(testData, "age", 43)

    assertThat(testData::class.getDeclaredPropertyValue(testData, "name")).isEqualTo("new-name")
    assertThat(testData::class.getDeclaredPropertyValue(testData, "age")).isEqualTo(43)
  }

  @Test
  fun testMissingFieldNamesClassAndDeclaredFields() {
    val testData = TestData("test", 42)

    assertFailure {
      TestData::class.java.getDeclaredFieldValue(testData, "non-existing-name")
    }.all {
      isInstanceOf<NoSuchFieldException>()
      hasMessage("Field 'non-existing-name' not found in class it.neckar.kotlin.reflect.JvmReflectExtTest\$TestData (declared fields: [age, name])")
      cause().isNotNull().isInstanceOf<NoSuchFieldException>()
    }
  }

  @Test
  fun testMissingPropertyNamesClassAndDeclaredProperties() {
    val testData = TestData("test", 42)

    assertFailure {
      testData::class.getDeclaredPropertyValue(testData, "non-existing-name")
    }.all {
      isInstanceOf<NoSuchFieldException>()
      hasMessage("Property 'non-existing-name' not found in class it.neckar.kotlin.reflect.JvmReflectExtTest\$TestData (declared properties: [age, description, name])")
    }
  }

  /**
   * The lookup runs on the receiver class - which may be a super class of the instance.
   * The message must name the class that was actually searched, and say that the instance is something else.
   */
  @Test
  fun testMissingPropertyNamesTheSearchedClassNotTheInstanceClass() {
    val testData = SubTestData("test")

    assertFailure {
      BaseTestData::class.getDeclaredPropertyValue(testData, "name")
    }.all {
      isInstanceOf<NoSuchFieldException>()
      hasMessage(
        "Property 'name' not found in class it.neckar.kotlin.reflect.JvmReflectExtTest\$BaseTestData (declared properties: [], " +
          "instance is a class it.neckar.kotlin.reflect.JvmReflectExtTest\$SubTestData)"
      )
    }
  }

  @Test
  fun testImmutablePropertyNamesTheProperty() {
    val testData = TestData("test", 42)

    assertFailure {
      TestData::class.getMutableProperty("name", testData)
    }.all {
      isInstanceOf<IllegalArgumentException>()
      hasMessage("Property 'name' in class it.neckar.kotlin.reflect.JvmReflectExtTest\$TestData is a val, not a var")
    }
  }

  @Test
  fun testPropertyWithoutBackingFieldNamesTheProperty() {
    val testData = TestData("test", 42)

    assertFailure {
      TestData::class.setDeclaredPropertyValue(testData, "description", "new-description")
    }.all {
      isInstanceOf<NoSuchFieldException>()
      hasMessage(
        "Property 'description' in class it.neckar.kotlin.reflect.JvmReflectExtTest\$TestData has no backing field " +
          "- only properties with a backing field can be set by reflection"
      )
    }
  }

  private data class TestData(
    private val name: String,
    private val age: Int
  ) {
    val description: String
      get() = "$name ($age)"
  }

  private open class BaseTestData

  private data class SubTestData(
    val name: String
  ) : BaseTestData()
}
