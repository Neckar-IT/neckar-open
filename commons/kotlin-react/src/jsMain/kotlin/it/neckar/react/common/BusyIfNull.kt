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
package it.neckar.react.common

import react.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Shows a busy indicator if the property is null.
 * Calls the handler with the value if it is not null.
 */
fun <T> RBuilder.busyIfNull(value: T?, small: Boolean = false, nonNullHandler: RElementBuilder<*>.(T) -> Unit) {
  contract {
    callsInPlace(nonNullHandler, InvocationKind.AT_MOST_ONCE)
  }

  return child(BusyIfNull) {
    attrs {
      this.busy = value == null
      this.small = small
    }

    if (value != null) {
      this.nonNullHandler(value)
    }
  }
}

fun <T1, T2> RBuilder.busyIfNull(value1: T1?, value2: T2?, small: Boolean = false, nonNullHandler: RElementBuilder<*>.(T1, T2) -> Unit) {
  contract {
    callsInPlace(nonNullHandler, InvocationKind.AT_MOST_ONCE)
  }

  return child(BusyIfNull) {
    attrs {
      this.busy = value1 == null || value2 == null
      this.small = small
    }

    if (value1 != null && value2 != null) {
      this.nonNullHandler(value1, value2)
    }
  }
}

fun <T1, T2, T3> RBuilder.busyIfNull(value1: T1?, value2: T2?, value3: T3?, small: Boolean = false, nonNullHandler: RElementBuilder<*>.(T1, T2, T3) -> Unit) {
  contract {
    callsInPlace(nonNullHandler, InvocationKind.AT_MOST_ONCE)
  }

  return child(BusyIfNull) {
    attrs {
      this.busy = value1 == null || value2 == null || value3 == null
      this.small = small
    }

    if (value1 != null && value2 != null && value3 != null) {
      this.nonNullHandler(value1, value2, value3)
    }
  }
}

val BusyIfNull: FC<BusyIfNullProps> = fc("busyIfNull") { props ->
  if (props.busy == true) {
    if (props.small == true) {
      BusyIndicatorSmall {}
    } else {
      BusyIndicator {}
    }
  } else {
    props.children()
  }
}

external interface BusyIfNullProps : PropsWithChildren {
  var busy: Boolean?
  var small: Boolean?
}
