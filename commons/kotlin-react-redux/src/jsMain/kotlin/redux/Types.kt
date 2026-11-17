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

external interface Action {
  val type: String
}

external interface Store<S, A, R> {
  fun getState(): S

  fun dispatch(action: A): R

  fun subscribe(listener: () -> Unit): () -> Unit

  fun replaceReducer(nextReducer: Reducer<S, A>)
}

external interface ReducerContainer<S, A>

external interface ActionCreatorContainer<A>

external interface BoundActionCreatorContainer<A, R>

external interface MiddlewareApi<S, A, R> {
  fun getState(): S

  fun dispatch(action: A): R
}

typealias Reducer<S, A> = (S, A) -> S

typealias StoreCreator<S, A, R> = (Reducer<S, A>, S) -> Store<S, A, R>

typealias Enhancer<S, A1, R1, A2, R2> = (StoreCreator<S, A1, R1>) -> StoreCreator<S, A2, R2>

typealias Middleware<S, A1, R1, A2, R2> = (MiddlewareApi<S, A1, R1>) -> ((A1) -> R1) -> (A2) -> R2
