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
import it.neckar.common.redux.Dispatch.registerDispatchAction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DispatchTest {
  @BeforeEach
  fun setUp() {
    Dispatch.disposeDispatchAction()
  }

  @AfterEach
  fun tearDown() {
    Dispatch.disposeDispatchAction()
  }

  @Test
  fun testUninitialized() {
    assertFailure {
      dispatch(object : StateAction<Int> {
        override val type: ChangeType = ChangeType.HistoryReset

        override fun Int.updateState(): Int {
          throw UnsupportedOperationException("Must not be called")
        }
      })
    }.messageContains("No dispatch action configured")
  }

  @Test
  fun testDispatchAction() {
    var state = ""

    registerDispatchAction { stateAction ->
      with(stateAction) {
        state = state.updateState()
      }
    }

    val action = object : StateAction<String> {
      override val type: ChangeType = ChangeType.HistoryReset

      override fun String.updateState(): String {
        return this + "a"
      }
    }

    assertThat(state).isEqualTo("")
    dispatch(action)
    assertThat(state).isEqualTo("a")
    dispatch(action)
    dispatch(action)
    assertThat(state).isEqualTo("aaa")
  }
}
