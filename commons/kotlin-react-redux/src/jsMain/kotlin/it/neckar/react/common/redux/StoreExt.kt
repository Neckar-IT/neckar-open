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
package it.neckar.react.common.redux

import it.neckar.common.redux.StateAction
import it.neckar.common.redux.UndoState
import it.neckar.common.redux.UndoStateAction
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import redux.RAction
import redux.Store


/**
 * Supports undo states
 */
@Suppress("UNCHECKED_CAST")
fun <S> undoStateReduceAction(state: UndoState<S>, action: RAction?): UndoState<S> {
  if (action == null) {
    //no action provided - just skip
    return state
  }

  return when (action) {
    is StateActionWrapper<*> -> {
      val stateActionWrapper = action as StateActionWrapper<S>
      val stateAction: StateAction<S> = stateActionWrapper.stateAction

      logger.debug("Reducing store with: ${stateAction::class.simpleName}")

      val currentState = state.currentState

      when (stateAction) {
        is UndoStateAction<*> -> {
          with(stateAction as UndoStateAction<S>) {
            return state.updateState()
          }
        }

        else -> {
          with(stateAction) {
            val newState = currentState.updateState()

            val actionType = stateAction.type

            return state.update(newState, actionType)
          }
        }
      }
    }

    else -> {
      logger.warn("Unsupported action: $action")
      state
    }
  }
}

/**
 * Redux Actions that automatically wraps a [StateAction]. Allows updating of the redux store
 */
data class StateActionWrapper<S>(
  /**
   * The wrapped state action that is executed
   */
  val stateAction: StateAction<S>,
) : RAction

/**
 * Dispatches a state action.
 */
fun <S, R> Store<S, RAction, R>.dispatch(action: StateAction<S>): R {
  return this.dispatch(StateActionWrapper(action))
}

private val logger: Logger = LoggerFactory.getLogger("it.neckar.react.common.redux.StoreExt")
