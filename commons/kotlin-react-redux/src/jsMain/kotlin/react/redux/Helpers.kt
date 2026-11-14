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

import js.objects.jso
import react.*

inline fun js(builder: dynamic.() -> Unit): dynamic =
  jso(builder)


fun <A, R> rConnect(
  options: (Options<Any, Props, Props, DispatchProps<A, R>>.() -> Unit) = {},
): HOC<DispatchProps<A, R>, Props> {
  return connect<Any, A, R, Props, Props, Props, DispatchProps<A, R>>(
    undefined,
    undefined,
    undefined,
    js {
      getDisplayName = { name: String -> "RConnect($name)" }
      methodName = "rConnect"
      options(this)
    }.unsafeCast<Options<Any, Props, Props, DispatchProps<A, R>>>()
  )
}

fun <S, OP : Props, P : Props> rConnect(
  mapStateToProps: P.(S, OP) -> Unit,
  options: (Options<S, OP, P, P>.() -> Unit) = {},
): HOC<P, OP> {
  return connect<S, Any, Any, OP, P, Props, P>(
    { state: S, ownProps: OP ->
      js {
        mapStateToProps(this, state, ownProps)
      }.unsafeCast<P>()
    },
    undefined,
    undefined,
    js {
      getDisplayName = { name: String -> "RConnect($name)" }
      methodName = "rConnect"
      options(this)
    }.unsafeCast<Options<S, OP, P, P>>()
  )
}

fun <A, R, OP : Props, P : Props> rConnect(
  mapDispatchToProps: P.((A) -> R, OP) -> Unit,
  options: (Options<Any, OP, Props, P>.() -> Unit) = {},
): HOC<P, OP> {
  return connect<Any, A, R, OP, Props, P, P>(
    undefined,
    { dispatch, ownProps ->
      js {
        mapDispatchToProps(this, dispatch, ownProps)
      }.unsafeCast<P>()
    },
    undefined,
    js {
      getDisplayName = { name: String -> "RConnect($name)" }
      methodName = "rConnect"
      options(this)
    }.unsafeCast<Options<Any, OP, Props, P>>()
  )
}

fun <S, A, R, OP : Props, SP : Props, DP : Props, P : Props> rConnect(
  mapStateToProps: SP.(S, OP) -> Unit,
  mapDispatchToProps: DP.((A) -> R, OP) -> Unit,
  options: (Options<S, OP, SP, P>.() -> Unit) = {},
): HOC<P, OP> {
  return connect<S, A, R, OP, SP, DP, P>(
    { state, ownProps ->
      js {
        mapStateToProps(this, state, ownProps)
      }.unsafeCast<SP>()
    },
    { dispatch, ownProps ->
      js {
        mapDispatchToProps(this, dispatch, ownProps)
      }.unsafeCast<DP>()
    },
    undefined,
    js {
      getDisplayName = { name: String -> "RConnect($name)" }
      methodName = "rConnect"
      options(this)
    }.unsafeCast<Options<S, OP, SP, P>>()
  )
}

fun <S, A, R, OP : Props, SP : Props, DP : Props, P : Props> rConnect(
  mapStateToProps: SP.(S, OP) -> Unit,
  mapDispatchToProps: DP.((A) -> R, OP) -> Unit,
  mergeProps: P.(SP, DP, OP) -> Unit,
  options: (Options<S, OP, SP, P>.() -> Unit) = {},
): HOC<P, OP> {
  return connect<S, A, R, OP, SP, DP, P>(
    { state, ownProps ->
      js {
        mapStateToProps(this, state, ownProps)
      }.unsafeCast<SP>()
    },
    { dispatch, ownProps ->
      js {
        mapDispatchToProps(this, dispatch, ownProps)
      }.unsafeCast<DP>()
    },
    { stateProps, dispatchProps, ownProps ->
      js {
        mergeProps(this, stateProps, dispatchProps, ownProps)
      }.unsafeCast<P>()
    },
    js {
      getDisplayName = { name: String -> "RConnect($name)" }
      methodName = "rConnect"
      options(this)
    }.unsafeCast<Options<S, OP, SP, P>>()
  )
}
