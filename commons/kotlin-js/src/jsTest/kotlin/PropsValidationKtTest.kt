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
import assertk.*
import assertk.assertions.*
import it.neckar.commons.kotlin.js.safeGet
import kotlin.test.Test

external interface MyPerson {
  var name: String
}

external class MyPersonClass {
  var name: String
}

class PropsValidationKtTest {

  @Test
  fun testCorrectType() {
    val jsObject = js("""{ 'name': 'max'}""")

    val testPerson = jsObject.unsafeCast<MyPerson>()
    val propertyName = testPerson::name

    assertThat(propertyName.get()).isNotNull()
    assertThat(propertyName.get()).isInstanceOf(String::class)

    assertThat(propertyName.safeGet()).isEqualTo("max")
  }

  @Test
  fun testWithUndefined() {
    val jsObject = js("{}")

    val testPerson = jsObject.unsafeCast<MyPerson>()
    val propertyName = testPerson::name

    assertThat(propertyName.get()).isEqualTo(null)
    assertThat(propertyName.get()).isNotInstanceOf(String::class)

    assertFailure {
      propertyName.safeGet()
    }.messageContains("is not set")
  }

  @Test
  fun testWithWrongType() {
    val jsObject = js("""{ 'name': 42}""")

    val testPerson = jsObject.unsafeCast<MyPerson>()
    val propertyName = testPerson::name

    assertThat(propertyName.get()).isNotNull()
    assertThat(propertyName.get()).isNotInstanceOf(String::class)

    assertFailure {
      propertyName.safeGet()
    }.messageContains("has invalid value")
  }

  @Test
  fun testWrongFieldName() {
    val jsObject = js("""{ 'invalidName': 'I Am Invalid'}""")

    val testPerson = jsObject.unsafeCast<MyPerson>()

    val propertyName = testPerson::name
    assertThat(propertyName.get()).isNull()

    assertFailure {
      propertyName.safeGet()
    }.messageContains("is not set")
  }

  @Test
  fun testExternalClass() {
    val jsObject = js("""{ 'name': 'Max Mustermann'}""")
    val testPerson = jsObject.unsafeCast<MyPersonClass>()
    val propertyName = testPerson::name

    assertThat(propertyName.safeGet()).isEqualTo("Max Mustermann")
  }
}
