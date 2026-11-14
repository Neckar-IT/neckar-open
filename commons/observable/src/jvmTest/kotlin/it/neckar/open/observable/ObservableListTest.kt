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
import it.neckar.open.collections.fastForEach
import it.neckar.open.dispose.Disposable
import it.neckar.open.test.utils.clearMock
import it.neckar.open.test.utils.mockWasCalled
import it.neckar.open.test.utils.mockWasNotCalled
import org.junit.jupiter.api.Test

/**
 *
 */
class ObservableListTest {
  @Test
  fun testApiNonList() {
    val observableList: ObservableObject<String> = ObservableObject("a")
    val mappedToLength = observableList.map {
      it.length
    }

    assertThat(mappedToLength.value).isEqualTo(1)
    observableList.value = "123545"
    assertThat(mappedToLength.value).isEqualTo(6)
  }

  @Test
  fun testApiNonListMapVsSelect() {
    val observableList: ObservableObject<ObservableObject<String>> = ObservableObject(ObservableObject("asdf"))
    val mappedToLength = observableList.map {
      it.value.length
    }

    val selectLength = observableList.select { it }.map { it.length }

    assertThat(mappedToLength.value).isEqualTo(4)
    assertThat(selectLength.value).isEqualTo(4)
    observableList.value.value = "123545"

    //Still 4, because the inner observable object is not observed
    assertThat(mappedToLength.value).isEqualTo(4)

    //Using select, the value is updated
    assertThat(selectLength.value).isEqualTo(6)
  }

  @Test
  fun testObservableListMap() {
    val observableList: ObservableList<Int> = ObservableList(listOf(1, 2, 3))

    val mapped = observableList.mapElements { it + 10 }
    assertThat(mapped.value).containsExactly(11, 12, 13)

    observableList.value = listOf(4, 5, 6)
    assertThat(mapped.value).containsExactly(14, 15, 16)
  }

  @Test
  fun testSelectList() {
    val observableList: ObservableList<ObservableObject<String>> = ObservableList(
      listOf(
        ObservableObject("a1"),
        ObservableObject("b22"),
        ObservableObject("c333")
      )
    )

    //This list is updated, whenever:
    //* one of the inner observable objects is updated
    //* the list itself is updated
    val selected = observableList.selectList { it }
    assertThat(selected.value).containsExactly("a1", "b22", "c333")

    //Register a callback
    val action: (newValue: List<String>) -> Unit = mockk(relaxed = true)
    selected.consume(action = action)
    assertThat(action).mockWasNotCalled()


    //change inner value
    observableList.value[1].value = "b222"
    assertThat(selected.value).containsExactly("a1", "b222", "c333")
    assertThat(action).mockWasCalled { action(listOf("a1", "b222", "c333")) }
    action.clearMock()


    //change list itself
    observableList.value = listOf(
      ObservableObject("a1"),
      ObservableObject("b22"),
      ObservableObject("c333"),
      ObservableObject("d4444")
    )
    assertThat(selected.value).containsExactly("a1", "b22", "c333", "d4444")
    assertThat(action).mockWasCalled { action(listOf("a1", "b22", "c333", "d4444")) }
    action.clearMock()


    //Change inner value again
    observableList.value[1].value = "b777"
    assertThat(selected.value).containsExactly("a1", "b777", "c333", "d4444")
    assertThat(action).mockWasCalled { action(listOf("a1", "b777", "c333", "d4444")) }
    action.clearMock()
  }

  @Test
  fun testRegisterInner() {
    val sourceList: ObservableList<ObservableObject<String>> = ObservableList(
      listOf(
        ObservableObject("a1"),
        ObservableObject("b22"),
        ObservableObject("c333")
      )
    )


    fun extractValues(): List<String> {
      val values = sourceList.value.map { it.value }
      return values
    }

    val selected = ObservableList<String>(extractValues())

    //Always update the results if one value changes
    sourceList.value.fastForEach {
      it.consume {
        selected.value = extractValues()
      }
    }

    assertThat(selected.value).containsExactly("a1", "b22", "c333")
    sourceList.value[1].value = "b222"
    assertThat(selected.value).containsExactly("a1", "b222", "c333")
  }

  @Test
  fun testSelectListObservable() {
    val observableList: ObservableList<Foo> = ObservableList(
      listOf(
        Foo("a1"),
        Foo("b22"),
        Foo("c333")
      )
    )

    val action = mockk<(Any?) -> Unit>(relaxed = true) // Create a mock for the action

    observableList.selectListObservable { it.nameProperty }.consume(action)
    assertThat(action).mockWasNotCalled()


    //Modify a single value
    observableList.value[1].nameProperty.value = "b222"
    assertThat(action).mockWasCalled { action("b222") }

    //Modify the list itself
    observableList.value = listOf(
      Foo("2a1"),
      Foo("2b22"),
      Foo("2c333"),
      Foo("2d4444")
    )

    assertThat(action).mockWasCalled { action(any()) }


    //Modify a single value again
    observableList.value[2].nameProperty.value = "foobar"
    assertThat(action).mockWasCalled { action("foobar") }
  }

  private class Foo(initialName: String) {
    val nameProperty = ObservableString(initialName)
  }


  @Test
  fun testInnerBug() {
    val inner = Inner()
    assertThat(inner.name).isEqualTo("theName")

    val propertyChangeListener: (Any?, Any?) -> Unit = mockk(relaxed = true)
    inner.nameProperty.consumeChanges(propertyChangeListener)

    val allPropertiesListener: (oldValue: Any?, newValue: Any?) -> Unit = mockk(relaxed = true)
    inner.consumeAllPropertiesChanges(allPropertiesListener)

    assertThat(propertyChangeListener).mockWasNotCalled()
    assertThat(allPropertiesListener).mockWasNotCalled()
    inner.name = "newName"
    assertThat(propertyChangeListener).mockWasCalled { propertyChangeListener("theName", "newName") }
    assertThat(allPropertiesListener).mockWasCalled { propertyChangeListener("theName", "newName") }
  }

  @Test
  fun testRecursive() {
    val outer = Outer()
    val action = mockk<(Any?, Any?) -> Unit>(relaxed = true)

    outer.consumeAllPropertiesChanges(action)
    assertThat(action).mockWasNotCalled()

    outer.inners[0].name = "newName"
    assertThat(action).mockWasCalled { action("theName", "newName") }
  }

  private class Outer : ObservableProperties {
    val innersProperty = ObservableList(
      listOf(Inner(), Inner())
    )
    var inners by innersProperty

    private val observablePropertiesSupport = ObservablePropertiesSupport.create {
      addObservableListSelected(innersProperty) { it }
    }

    override fun consumeAllPropertiesChanges(action: ConsumeChangesAction<Any?>): Disposable {
      return observablePropertiesSupport.consumeAllPropertiesChanges(action)
    }
  }

  private class Inner : ObservableProperties {
    val nameProperty = ObservableString("theName")
    var name by nameProperty

    private val observablePropertiesSupport = ObservablePropertiesSupport.create {
      addObservable(nameProperty)
    }

    override fun consumeAllPropertiesChanges(action: ConsumeChangesAction<Any?>): Disposable {
      return observablePropertiesSupport.consumeAllPropertiesChanges(action)
    }
  }
}
