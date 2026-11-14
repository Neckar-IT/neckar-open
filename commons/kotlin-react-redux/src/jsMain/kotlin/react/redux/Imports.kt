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
@file:JsModule("react-redux")
@file:JsNonModule

package react.redux

import react.*
import redux.Store

external val Provider: FC<ProviderProps>

external interface ProviderProps : PropsWithChildren {
  var store: Store<*, *, *>
  var context: Context<*>
}

external fun <S, A, R, OP : Props, SP : Props, DP : Props, P : Props> connect(
  mapStateToProps: ((S, OP) -> SP)? = definedExternally,
  mapDispatchToProps: (((A) -> R, OP) -> DP)? = definedExternally,
  mergeProps: ((SP, DP, OP) -> P)? = definedExternally,
  options: Options<S, OP, SP, P>? = definedExternally,
): HOC<P, OP>

external fun <S, A, R, OP : Props, P : Props> connectAdvanced(
  selectorFactory: SelectorFactory<S, A, R, OP, P>,
  options: ConnectOptions<P> = definedExternally,
): HOC<P, OP>

external fun batch(action: () -> Unit)

external fun <S, R> useSelector(selector: (S) -> R): R

external fun <S, R> useSelector(selector: (S) -> R, equalityFn: (R, R) -> Boolean): R

external fun <A, R> useDispatch(): ((A) -> R)

external fun <S, A, R> useStore(): Store<S, A, R>

external fun <S, R> createSelectorHook(context: Context<*>): (selector: (S) -> R, equalityFn: ((R, R) -> Boolean)) -> R

external fun <A, R> createDispatchHook(context: Context<*>): () -> ((A) -> R)

external fun <S, A, R> createStoreHook(context: Context<*>): () -> Store<S, A, R>
