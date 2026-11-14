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
@file:Suppress("RETURN_VALUE_NOT_USED") // Test-scope subscriptions don't need lifecycle management

package it.neckar.open.observable

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertFails

/**
 */
internal class ObservableObjectTest {
  @Test
  fun testMap2() {
    val observableObject0 = ObservableObject(7)
    val observableObject1 = ObservableObject("asdf")
    val observableObject2 = ObservableObject(27.0)

    val mapped = observableObject0.map(
      observableObject1,
      observableObject2,
    ) { a, b, c ->
      "$a - $b - $c"
    }

    assertThat(mapped.value).isEqualTo("7 - asdf - 27.0")
    observableObject0.value = 8
    assertThat(mapped.value).isEqualTo("8 - asdf - 27.0")
    observableObject1.value = "foo"
    assertThat(mapped.value).isEqualTo("8 - foo - 27.0")
    observableObject2.value = 11.0
    assertThat(mapped.value).isEqualTo("8 - foo - 11.0")
  }

  @Test
  internal fun testMapWithEvent() {
    val observableObject = ObservableObject("Hey")
    val lengthProperty = observableObject.map {
      it.length
    }

    assertThat(lengthProperty.value).isEqualTo(3)

    var result = -1
    var called = 0

    lengthProperty.consume(false) {
      ++called
      result = it
    }

    //Not yet called!
    assertThat(called).isEqualTo(0)
    assertThat(result).isEqualTo(-1)

    observableObject.value = "foobar"
    assertThat(lengthProperty.value).isEqualTo(6)

    assertThat(called).isEqualTo(1)
    assertThat(result).isEqualTo(6)
  }

  @Test
  fun testSameValueEvent() {
    val observableObject = ObservableObject("Hey")

    val updates = mutableListOf<String>()
    observableObject.consume {
      updates.add(it)
    }

    assertThat(updates).isEmpty()

    observableObject.value = "Hey"
    assertThat(updates).isEmpty() //no update expected, since it is the same value

    observableObject.value = "Other"
    assertThat(updates).containsExactly("Other") //Update
  }

  @Test
  fun testGenericsReadOnly() {
    var number: ReadOnlyObservableObject<Number> = ObservableObject<Number>(17.0)
    val double: ReadOnlyObservableObject<Double> = ObservableObject(17.0)
    val int: ReadOnlyObservableObject<Int> = ObservableObject(17)

    //Assignments to number should be possible
    number = double
    number = int
  }

  @Test
  internal fun testDelegates() {
    val observableObject = ObservableObject("asdf")
    assertThat(observableObject.value).isEqualTo("asdf")

    observableObject.value = "2222"

    assertThat(observableObject.value).isEqualTo("2222")
  }

  @Test
  internal fun testDelegate() {
    val myObject = object : Any() {
      val nameProperty = ObservableObject("asdf")
      var name by nameProperty
    }

    assertThat(myObject.name).isEqualTo("asdf")
    myObject.name = "777"
    assertThat(myObject.name).isEqualTo("777")
  }

  @Test
  internal fun testBidirectionalBinding() {
    val first = ObservableObject("1")
    val second = ObservableObject("2")

    first.bindBidirectional(second)

    assertThat(first.value).isEqualTo("2")
    assertThat(second.value).isEqualTo("2")

    first.value = "77"

    assertThat(first.value).isEqualTo("77")
    assertThat(second.value).isEqualTo("77")

    second.value = "99"

    assertThat(first.value).isEqualTo("99")
    assertThat(second.value).isEqualTo("99")
  }

  @Test
  internal fun testUnidirectionalBinding() {
    val first = ObservableObject("1")
    val second = ObservableObject("2")

    first.bind(second)

    assertThat(first.value).isEqualTo("2")
    assertThat(second.value).isEqualTo("2")

    assertFailure {
      first.value = "a"
    }.hasMessage("Setting the value to <a> is not allowed - this observable is already bound to another observable. Current value: <2>")

    assertThat(first.value).isEqualTo("2")
    assertThat(second.value).isEqualTo("2")

    second.value = "b"
    assertThat(first.value).isEqualTo("b")
    assertThat(second.value).isEqualTo("b")
  }

  @Test
  internal fun testBindingTwiceReportsBothObservables() {
    val target = ObservableObject("1")
    val first = ObservableObject("2")
    val second = ObservableObject("3")

    target.bind(first)

    assertFailure {
      target.bind(second)
    }.hasMessage("Binding to <ObservableObject [value: 3]> is not allowed - this observable is already bound to another observable. Current value: <2>")
  }

  @Test
  internal fun testMarkAsBoundTwiceReportsTheValue() {
    val target = ObservableObject("1")
    target.markAsBound()

    assertFailure {
      target.markAsBound()
    }.hasMessage("Marking as bound is not allowed - this observable is already bound to another observable. Current value: <1>")
  }

  @Test
  internal fun testBindTwo() {
    val enabled = ObservableBoolean(false)
    val count = ObservableInt(0)


    val result: ReadOnlyObservableObject<String> = enabled.map(count) { enabledValue, countValue ->
      "$enabledValue - $countValue"
    }

    assertThat(result.value).isEqualTo("false - 0")
    enabled.value = true
    assertThat(result.value).isEqualTo("true - 0")
    count.value = 17
    assertThat(result.value).isEqualTo("true - 17")

    //must not be published, when no value has been changed
    result.consumeChanges { _, _ ->
      fail("must not be called")
    }

    count.value = 17
    enabled.value = true
  }

  @Test
  internal fun testMap() {
    val enabled = ObservableBoolean(false)

    val mapped = enabled.map {
      it.toString()
    }

    assertThat(mapped.value).isEqualTo("false")

    mapped.consumeChanges { _, _ ->
      fail("must not be called")
    }

    //Repeat the same value - must not be published
    enabled.value = false
    enabled.value = false
    enabled.value = false
  }

  @Test
  internal fun testMerge() {
    val o1 = ObservableBoolean(false)
    val o2 = ObservableBoolean(false)
    val o3 = ObservableBoolean(false)

    val merged = reduceObservables(o1, o2, o3) {
      it.reduce { b1, b2 ->
        b1 || b2
      }
    }

    assertThat(merged.value).isFalse()

    o1.value = false
    o2.value = true
    o3.value = false
    assertThat(merged.value).isTrue()

    o1.value = false
    o2.value = false
    o3.value = false
    assertThat(merged.value).isFalse()

    o1.value = false
    o2.value = false
    o3.value = true
    assertThat(merged.value).isTrue()
  }

  @Test
  internal fun testReduce2() {
    val o1 = ObservableBoolean(false)
    val o2 = ObservableBoolean(false)
    val o3 = ObservableBoolean(false)

    val merged = o1.reduce(o2, o3) {
      it.reduce { b1, b2 ->
        b1 || b2
      }
    }

    assertThat(merged.value).isFalse()

    o1.value = false
    o2.value = true
    o3.value = false
    assertThat(merged.value).isTrue()

    o1.value = false
    o2.value = false
    o3.value = false
    assertThat(merged.value).isFalse()

    o1.value = false
    o2.value = false
    o3.value = true
    assertThat(merged.value).isTrue()
  }

  @Test
  fun testConsumeImmediately() {
    val observableInt = ObservableInt(4711)
    var result = 1
    var called = 0
    observableInt.consume(true) {
      ++called
      result = it
    }
    assertThat(result).isEqualTo(4711)
    assertThat(called).isEqualTo(1)

    observableInt.value = 666
    assertThat(result).isEqualTo(666)
    assertThat(called).isEqualTo(2)
  }

  @Test
  fun testConsumeImmediately2() {
    val observableInt = ObservableInt(4711)
    var result = 1
    var called = 0
    observableInt.consume(false) {
      ++called
      result = it
    }

    assertThat(result).isEqualTo(1)
    assertThat(called).isEqualTo(0)

    observableInt.value = 4711
    assertThat(result).isEqualTo(1)
    assertThat(called).isEqualTo(0)

    observableInt.value = 666
    assertThat(result).isEqualTo(666)
    assertThat(called).isEqualTo(1)
  }
}
