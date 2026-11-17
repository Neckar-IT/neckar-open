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
package react.redux

import react.ComponentClass
import react.Props

external interface ConnectOptions<P : Props> : FactoryOptions<P> {
  var getDisplayName: ((String) -> String)?
  var methodName: String?
  var renderCountProp: String?
  var shouldHandleStateChanges: Boolean?
  var storeKey: String?
  var withRef: Boolean?
}

external interface Options<S, OP : Props, SP : Props, P : Props> : ConnectOptions<P> {
  var pure: Boolean?
  var areStatesEqual: ((S, S) -> Boolean)?
  var areOwnPropsEqual: ((OP, OP) -> Boolean)?
  var areStatePropsEqual: ((SP, SP) -> Boolean)?
  var areMergedPropsEqual: ((P, P) -> Boolean)?
}

external interface FactoryOptions<P : Props> {
  var displayName: String?
  var wrappedComponentName: String?
  var wrappedComponent: ComponentClass<P>?
}

typealias Selector<S, OP, P> = (S, OP) -> P

typealias SelectorFactory<S, A, R, OP, P> = ((A) -> R, FactoryOptions<P>) -> Selector<S, OP, P>

external interface DispatchProps<A, R> : Props {
  var dispatch: (A) -> R
}
