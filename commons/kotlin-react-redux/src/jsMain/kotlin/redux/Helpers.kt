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
package redux

import js.objects.Object
import js.objects.jso

inline fun js(builder: dynamic.() -> Unit): dynamic =
  jso(builder)

fun <T : Any> clone(obj: T): T = Object.assign(jso(), obj)

inline fun <T : Any> assign(obj: T, builder: T.() -> Unit): T = clone(obj).apply(builder)


val <S> Store<S, *, *>.state: S get() = getState()

fun <S, A> combineReducers(reducers: Map<String, Reducer<*, A>>): Reducer<S, A> = combineReducers(js {
  reducers.forEach { this[it.key] = it.value }
}.unsafeCast<ReducerContainer<S, A>>())

fun <A, R> bindActionCreators(actionCreators: Map<String, (Array<Any>) -> A>, dispatch: (A) -> R): Map<String, (Array<Any>) -> R> {
  val result = mutableMapOf<String, (Array<Any>) -> R>()
  with(bindActionCreators(js {
    actionCreators.forEach { this[it.key] = it.value }
  }.unsafeCast<ActionCreatorContainer<A>>(), dispatch)) {
    Object.keys(this).forEach {
      result[it] = asDynamic()[it]
    }
  }
  return result
}

fun <S> rEnhancer(): Enhancer<S, Action, Action, RAction, WrapperAction> = { next ->
  { reducer, initialState ->
    fun wrapperReducer(reducer: Reducer<S, RAction>): Reducer<S, WrapperAction> {
      return { state, action -> reducer(state, action.action) }
    }

    val store = (next.unsafeCast<StoreCreator<S, WrapperAction, WrapperAction>>())(wrapperReducer(reducer), initialState)
    assign(Object.assign(js {}, store)) {
      dispatch = { action: RAction ->
        val result = store.dispatch(js {
          type = action::class.simpleName
          this.action = action
        }.unsafeCast<WrapperAction>())
        result
      }
      replaceReducer = { nextReducer: Reducer<S, RAction> ->
        store.replaceReducer(wrapperReducer(nextReducer))
      }
    }.unsafeCast<Store<S, RAction, WrapperAction>>()
  }
}

external interface WrapperAction : Action {
  override val type: String
  val action: RAction
}

external interface RAction
