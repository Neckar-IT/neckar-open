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
@file:JsModule("redux")
@file:JsNonModule

package redux

external fun <S> createStore(
  reducer: Reducer<S, Action>,
  preloadedState: S,
): Store<S, Action, Action>

external fun <S, A, R> createStore(
  reducer: Reducer<S, A>,
  preloadedState: S,
  enhancer: Enhancer<S, Action, Action, A, R>,
): Store<S, A, R>

external fun <S, A> combineReducers(reducers: ReducerContainer<*, A>): Reducer<S, A>

external fun <A, R> bindActionCreators(actionCreators: ActionCreatorContainer<A>, dispatch: (A) -> R): BoundActionCreatorContainer<A, R>

external fun <S, A1, R1, A2, R2> applyMiddleware(vararg middlewares: Middleware<S, A1, R1, A2, R2>): Enhancer<S, A1, R1, A2, R2>

external fun <A, T1, R> compose(function1: (T1) -> R, function2: (A) -> T1): (A) -> R

external fun <A, T1, T2, R> compose(function1: (T2) -> R, function2: (T1) -> T2, function3: (A) -> T1): (A) -> R

external fun <A, T1, T2, T3, R> compose(function1: (T3) -> R, function2: (T2) -> T3, function3: (T1) -> T2, function4: (A) -> T1): (A) -> R
