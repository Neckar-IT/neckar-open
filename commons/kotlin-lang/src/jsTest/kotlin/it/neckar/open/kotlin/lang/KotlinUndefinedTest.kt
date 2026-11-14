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
import it.neckar.open.isUndefined
import kotlin.test.Test

/**
 *
 */
class KotlinUndefinedTest {
  @Test
  fun testExtensionIsUndefined() {
    assertThat(actual = (js("undefined") as Any?).isUndefined()).isTrue()
    assertThat(actual = (js("null") as Any?).isUndefined()).isFalse()
    assertThat(actual = (js("'foobar'") as Any?).isUndefined()).isFalse()
  }

  @Test
  fun testMethodAny() {
    assertThat(actual = isUndefined((js("undefined") as Any?))).isTrue()
    assertThat(actual = isUndefined((js("null") as Any?))).isFalse()
    assertThat(actual = isUndefined((js("'foobar'") as Any?))).isFalse()
  }

  @Test
  fun testMethodDynamic() {
    assertThat(actual = isUndefined(js("undefined"))).isTrue()
    assertThat(actual = isUndefined(js("null"))).isFalse()
    assertThat(actual = isUndefined(js("'foobar'"))).isFalse()
  }

  @kotlin.test.Ignore
  @Test
  fun testUndefined() {
    println("Undefined")
    println("-------------------------------")
    callChecks(js("undefined"))
    println("-------------------------------")
  }

  @kotlin.test.Ignore
  @Test
  fun testNull() {
    println("Null")
    println("-------------------------------")
    callChecks(js("null"))
    println("-------------------------------")
  }

  private fun callChecks(jsDynamic: Any?) {
    println(jsDynamic != null)
    println(jsDynamic == null)
    println(jsDynamic == undefined)

    assertThat<Any?>(actual = jsDynamic).isNull()
    assertThat<Any?>(actual = jsDynamic).isEqualTo(undefined)
    assertThat<Any?>(actual = null).isEqualTo(undefined)


    val isUndefined = jsDynamic === undefined
    val isUndefinedEquals = jsDynamic == undefined

    val isNull = jsDynamic == null
    println("isUndefined: $isUndefined, isUndefinedEquals: $isUndefinedEquals, isNull: $isNull")
  }
}
