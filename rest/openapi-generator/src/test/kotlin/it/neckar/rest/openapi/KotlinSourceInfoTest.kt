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
package it.neckar.rest.openapi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.KotlinSourceInfo

/**
 * Tests for KotlinSourceInfo.create() method used in production code
 * (OpenApiKtorExt.kt:29)
 */
class KotlinSourceInfoTest {

  @Test
  fun testCreateWithValidStackTraceElement() {
    val stackTraceElement = StackTraceElement(
      "com.example.MyClass",
      "myMethod",
      "MyClass.kt",
      42
    )

    val sourceInfo = KotlinSourceInfo.create(stackTraceElement)

    assertThat(sourceInfo).isNotNull()
    assertThat(sourceInfo!!.className).isEqualTo("com.example.MyClass")
    assertThat(sourceInfo.methodName).isEqualTo("myMethod")
    assertThat(sourceInfo.fileName).isEqualTo("MyClass.kt")
    assertThat(sourceInfo.lineNumber).isEqualTo(42)
    assertThat(sourceInfo.location).isEqualTo("MyClass.kt:42")
  }

  @Test
  fun testCreateWithNullStackTraceElement() {
    val sourceInfo = KotlinSourceInfo.create(null)

    assertThat(sourceInfo).isNull()
  }

  @Test
  fun testLocationProperty() {
    val sourceInfo = KotlinSourceInfo(
      fileName = "TestFile.kt",
      lineNumber = 123,
      className = "TestClass",
      methodName = "testMethod"
    )

    assertThat(sourceInfo.location).isEqualTo("TestFile.kt:123")
  }
}
