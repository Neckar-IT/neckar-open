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
import io.mockk.mockk
import it.neckar.open.dispose.Disposable
import it.neckar.open.test.utils.clearMock
import it.neckar.open.test.utils.mockWasCalled
import it.neckar.open.test.utils.mockWasNotCalled
import org.junit.jupiter.api.Test

/**
 *
 */
class ObservablePropertiesSupportTest {
  @Test
  fun testConsumeAllProperties() {
    val foo = Foo()

    val action = mockk<(Any?) -> Unit>(relaxed = true) // Create a mock for the action

    foo.consumeAllProperties(action)
    assertThat(action).mockWasNotCalled()


    foo.foobarProperty.value = "Bar"
    assertThat(action).mockWasCalled { action("Bar") }
    action.clearMock()

    foo.foobarProperty.value = "Bar" //same value, no change!
    assertThat(action).mockWasNotCalled()
    action.clearMock()

    foo.barsProperty.value = listOf(Bar())
    assertThat(action).mockWasCalled { action(foo.bars) }
    action.clearMock()

    //update the inner property
    foo.bars[0].nameProperty.value = "Bar2"
    assertThat(action).mockWasCalled { action("Bar2") }
    action.clearMock()
  }

  @Test
  fun testBar() {
    val bar: Bar = Bar()
    val action: (oldValue: Any?, newValue: Any?) -> Unit = mockk(relaxed = true)

    bar.consumeChanges(action = action)
    assertThat(action).mockWasNotCalled()

    bar.nameProperty.value = "Bar2"
    assertThat(action).mockWasCalled { action("Bar", "Bar2") }
  }

  /**
   * Pins the snapshot semantics of notifyChange. A listener that disposes itself
   * during the call must not corrupt the iteration over [valueChangeListeners].
   */
  @Test
  fun `listener disposing itself during notify does not break further notifications`() {
    val foobarProperty = ObservableString("initial")
    val support = ObservablePropertiesSupport(foobarProperty)

    val secondCalled = mutableListOf<Any?>()

    var firstSubscription: Disposable? = null
    firstSubscription = support.consumeAllPropertiesChanges { _, _ ->
      // First listener disposes itself during the call — modifies the live list.
      firstSubscription?.dispose()
    }

    support.consumeAllPropertiesChanges { _, newValue ->
      secondCalled += newValue
    }

    // Before the fix this could fail with ConcurrentModificationException on JVM.
    foobarProperty.value = "updated"

    assertThat(secondCalled).isEqualTo(listOf<Any?>("updated"))
  }

  private class Foo : ObservableProperties {
    val barsProperty = ObservableList<Bar>(emptyList())
    var bars by barsProperty

    val foobarProperty: ObservableObject<String> = ObservableString("Foobar")
    var foobar by foobarProperty


    private val observablePropertiesSupport = ObservablePropertiesSupport.create {
      addObservableListSelected(barsProperty) { it }
      addObservable(foobarProperty)
    }

    override fun consumeAllPropertiesChanges(action: ConsumeChangesAction<Any?>): Disposable {
      return observablePropertiesSupport.consumeAllPropertiesChanges(action)
    }
  }

  private class Bar : ObservableProperties {
    val nameProperty = ObservableString("Bar")
    val ageProperty = ObservableInt(99)

    private val observablePropertiesSupport = ObservablePropertiesSupport(
      nameProperty,
      ageProperty,
    )

    override fun consumeAllPropertiesChanges(action: ConsumeChangesAction<Any?>): Disposable {
      return observablePropertiesSupport.consumeAllPropertiesChanges(action)
    }
  }
}
