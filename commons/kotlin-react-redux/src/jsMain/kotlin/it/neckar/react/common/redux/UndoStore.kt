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

import it.neckar.common.redux.RedoAction
import it.neckar.common.redux.StateAction
import it.neckar.common.redux.UndoAction
import it.neckar.common.redux.UndoState
import it.neckar.common.redux.dispatch
import it.neckar.logging.LoggerFactory
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.redux.*
import react.redux.ProviderProps
import redux.RAction
import redux.Store
import redux.rEnhancer
import redux.state

/**
 * Wrapper class for a store including undo functionality
 */
data class UndoStore<S>(
  /**
   * The wrapped React redux store
   */
  val store: Store<UndoState<S>, RAction, dynamic>,
) {

  /**
   * Returns the undo state of this store
   */
  val undoState: UndoState<S>
    get() = store.state

  /**
   * Returns the current state
   */
  val state: S
    get() {
      val undoState = undoState
      return undoState.currentState
    }

  /**
   * Subscribe to changes in the store. Returns the unsubscribe handle from the wrapped Redux
   * store; callers that keep the subscription for the lifetime of the store may discard it.
   */
  @IgnorableReturnValue
  fun subscribe(function: (store: UndoStore<S>) -> Unit): () -> Unit {
    return store.subscribe {
      function(this)
    }
  }

  /**
   * Dispatches the provided state action to this store
   */
  fun dispatch(stateAction: StateAction<S>) {
    store.dispatch(StateActionWrapper(stateAction))
  }

  companion object {
    /**
     * Creates a store that supports undo/redo
     */
    fun <S> create(
      preloadedState: S,
      /**
       * The max history length
       */
      maxHistoryLength: Int = 100,
    ): UndoStore<S> {
      val store: Store<UndoState<S>, RAction, dynamic> = redux.createStore<UndoState<S>, RAction, dynamic>(
        reducer = ::undoStateReduceAction, preloadedState = UndoState(preloadedState, maxHistoryLength = maxHistoryLength), enhancer = rEnhancer()
      ).also { store ->
        store.subscribe {
          logger.debug("Store updated", store)
        }
      }

      return UndoStore(store)
    }

    private val logger = LoggerFactory.getLogger("it.neckar.react.common.redux.UndoStore")
  }
}

/**
 * Registers the key event listeners for Undo/Redo
 */
fun UndoStore<*>.registerUndoRedoKeyHandling() {
  window.addEventListener("keydown", { event ->
    event as KeyboardEvent

    if (event.ctrlKey && event.shiftKey && (event.key == "z" || event.key == "Z")) {
      event.preventDefault()
      dispatch(RedoAction)
      return@addEventListener
    }

    if (event.ctrlKey && event.key == "z") {
      event.preventDefault()
      dispatch(UndoAction)
      return@addEventListener
    }
  })
}

/**
 * Provides an undo store to the children
 */
fun RBuilder.provider(
  store: UndoStore<*>,
  context: Context<*>? = null,
  handler: RHandler<ProviderProps>,
) {
  Provider {
    attrs.store = store.store
    if (context != null) attrs.context = context
    handler()
  }
}

/**
 * Use the current state of an undo store
 */
inline fun <S, R> useUndoStoreSelector(crossinline selector: S.() -> R): R {
  return useSelector<UndoState<S>, R> {
    val current = it.currentState
    selector(current)
  }
}

/**
 * Use the current state of an undo store
 * and allows to provide a custom equality function
 */
inline fun <S, R> useUndoStoreSelector(crossinline selector: S.() -> R, noinline equalityFn: (R, R) -> Boolean): R {
  val selectorFunction: (UndoState<S>) -> R = {
    val current = it.currentState
    selector(current)
  }
  return useSelector<UndoState<S>, R>(selectorFunction, equalityFn)
}

/**
 * Returns the undoable snapshots, oldest first.
 * The current snapshot is not part of it - use [useUndoStoreCurrent] for that one.
 */
fun <S> useUndoStoreHistory(): List<UndoState.Snapshot<S>> {
  return useSelector<UndoState<S>, List<UndoState.Snapshot<S>>> {
    it.previous
  }
}

/**
 * Returns the current snapshot - including its change type and label.
 * Use [useUndoStoreCurrentState] when only the state itself is needed.
 */
fun <S> useUndoStoreCurrent(): UndoState.Snapshot<S> {
  return useSelector<UndoState<S>, UndoState.Snapshot<S>> {
    it.current
  }
}

/**
 * Returns the current state of the undo store
 */
fun <S> useUndoStoreCurrentState(): S {
  return useSelector<UndoState<S>, S> {
    it.currentState
  }
}

/**
 * Returns the redoable snapshots, oldest first - the states that [UndoState.undo] has moved out of the way.
 * Empty unless an undo happened, because every state update discards the redo history.
 */
fun <S> useUndoStoreFuture(): List<UndoState.Snapshot<S>> {
  return useSelector<UndoState<S>, List<UndoState.Snapshot<S>>> {
    it.future
  }
}
