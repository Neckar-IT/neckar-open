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
package it.neckar.editHistory

import it.neckar.open.time.nowMillis
import it.neckar.open.unit.si.ms
import it.neckar.user.UserLoginName
import kotlinx.serialization.Serializable

/**
 * Wraps a position
 * When changes this position, call the [with] method to automatically extend the [editHistory]
 */
@Serializable
data class PositionEditHistory<T>(
  /**
   * Most recent [PositionEdit]
   * Contains the current value, time it was edited, and name of the editor
   */
  val currentEdit: PositionEdit<T>,
  /**
   * All other, previous [PositionEdit]s for this [PositionEditHistory]
   */
  val editHistory: List<PositionEdit<T>>,
) {

  // TODO history nicht empty list
  constructor(newEntry: T, loginName: UserLoginName?, editTime: @ms Double) : this(PositionEdit(newEntry, loginName, editTime), emptyList())

  constructor(newEntry: T, loginName: UserLoginName?) : this(PositionEdit(newEntry, loginName, nowMillis()), emptyList())

  constructor(newEntry: T, loginName: UserLoginName?, previousEdits: List<PositionEdit<T>>, editTime: @ms Double) : this(PositionEdit(newEntry, loginName, editTime), previousEdits)

  val currentValue: T
    get() = currentEdit.value

  val lastEditedTime: @ms Double
    get() = currentEdit.editTime

  val allEdits: List<PositionEdit<T>>
    get() = buildList {
      add(currentEdit)
      addAll(editHistory)
    }

  operator fun plus(otherProcessState: PositionEditHistory<T>?): PositionEditHistory<T> {
    return otherProcessState?.copy(editHistory = otherProcessState.editHistory + allEdits) ?: this
  }

  /**
   * *ALWAYS* call this function to extend this [PositionEditHistory] instead of [copy]
   */
  fun with(newEntry: T, loginName: UserLoginName, editTime: @ms Double = nowMillis()): PositionEditHistory<T> {
    if (newEntry == currentValue) return this
    return PositionEditHistory(newEntry = newEntry, loginName = loginName, previousEdits = allEdits, editTime = editTime)
  }

}
