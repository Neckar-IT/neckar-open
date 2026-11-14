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
package it.neckar.common.redux

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class UndoStateTest {
  private val state0 = MyState("initial")
  private val state1 = MyState("1")
  private val state2 = MyState("2")
  private val state3 = MyState("3")

  @Test
  fun testHistoryCategory() {
    var state = UndoState(state0, maxHistoryLength = 10)
    assertThat(state.previous).isEmpty()
    assertThat(state.currentState).isSameAs(state0)
    assertThat(state.future).isEmpty()

    state = state.update(state1, ChangeType.MinorChange)

    assertThat(state.previous.states()).containsExactly(state0)
    assertThat(state.previous.types()).containsExactly(ChangeType.HistoryReset)
    assertThat(state.currentState).isSameAs(state1)
    assertThat(state.future).isEmpty()
  }

  @Test
  fun testMinorCondense() {
    var state = UndoState(state0, maxHistoryLength = 10)
    state = state.update(state1, ChangeType.MinorChange)
    state = state.update(state2, ChangeType.MinorChange)

    assertThat(state.previous.states()).containsExactly(state0, state1)

    state = state.update(state3, ChangeType.MajorChange)
    assertThat(state.previous.states()).containsExactly(state0)
  }

  @Test
  fun testUndo() {
    var state = UndoState(state0, maxHistoryLength = 10)
    assertThat(state.previous).isEmpty()
    assertThat(state.currentState).isSameAs(state0)
    assertThat(state.future).isEmpty()

    state = state.update(state1, ChangeType.MinorChange)
    assertThat(state.previous.states()).containsExactly(state0)
    assertThat(state.currentState).isSameAs(state1)
    assertThat(state.future).isEmpty()

    state = state.undo()
    assertThat(state.previous).isEmpty()
    assertThat(state.currentState).isSameAs(state0)
    assertThat(state.future.states()).containsExactly(state1)
  }

  @Test
  fun `minor changes never grow previous beyond maxHistoryLength`() {
    var state = UndoState(state0, maxHistoryLength = 2)

    //Apply more minor changes than the history can hold.
    state = state.update(state1, ChangeType.MinorChange)
    state = state.update(state2, ChangeType.MinorChange)
    state = state.update(state3, ChangeType.MinorChange)

    //previous must be capped at maxHistoryLength, keeping the newest entries.
    assertThat(state.previous.states()).containsExactly(state1, state2)
    assertThat(state.currentState).isSameAs(state3)
  }

  private fun List<UndoState.Snapshot<MyState>>.states(): List<MyState> {
    return this.map { it.state }
  }

  private fun List<UndoState.Snapshot<MyState>>.types(): List<ChangeType> {
    return this.map { it.changeType }
  }

  @Test
  fun testUndoRedo() {
    var state = UndoState(state0, maxHistoryLength = 10)
    assertThat(state.previous).isEmpty()
    assertThat(state.currentState).isEqualTo(state0)
    assertThat(state.future).isEmpty()

    state = state.update(state1, ChangeType.MinorChange)
    state = state.update(state2, ChangeType.MinorChange)
    state = state.update(state3, ChangeType.MinorChange)

    assertThat(state.previous.states()).containsExactly(state0, state1, state2)
    assertThat(state.currentState).isSameAs(state3)
    assertThat(state.future).isEmpty()

    state = state.undo()
    assertThat(state.previous.states()).containsExactly(state0, state1)
    assertThat(state.currentState).isSameAs(state2)
    assertThat(state.future.states()).containsExactly(state3)

    state = state.undo()
    assertThat(state.previous.states()).containsExactly(state0)
    assertThat(state.currentState).isSameAs(state1)
    assertThat(state.future.states()).containsExactly(state2, state3)

    //Redo

    state = state.redo()
    assertThat(state.previous.states()).containsExactly(state0, state1)
    assertThat(state.currentState).isSameAs(state2)
    assertThat(state.future.states()).containsExactly(state3)

    state = state.redo()
    assertThat(state.previous.states()).containsExactly(state0, state1, state2)
    assertThat(state.currentState).isSameAs(state3)
    assertThat(state.future.states()).containsExactly()
  }
}

data class MyState(val name: String)
