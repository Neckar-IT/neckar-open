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
package it.neckar.common.redux

import it.neckar.open.unit.other.Order
import it.neckar.open.unit.other.Sorted

/**
 * Represents a state that can be undone
 */
data class UndoState<S>(
  /**
   * The snapshot for the current state
   */
  val current: Snapshot<S>,

  /**
   * The previous states (for undo).
   *
   * The first element is the *oldest* state.
   * The last element is the *newest* state.
   */
  val previous: @Sorted(Order.ASC) List<Snapshot<S>> = emptyList(),

  /**
   * The future states (for redo).
   *
   * The first element is the *oldest* state.
   * The last element is the *newest* state.
   */
  val future: @Sorted(Order.ASC) List<Snapshot<S>> = emptyList(),

  /**
   * The max history length (also limits the future)
   */
  val maxHistoryLength: Int,
) {

  constructor(
    initialState: S,
    maxHistoryLength: Int,
  ) : this(
    current = Snapshot(initialState, ChangeType.HistoryReset, null),
    maxHistoryLength = maxHistoryLength,
  )

  /**
   * Returns the current state
   */
  val currentState: S
    get() {
      return current.state
    }

  /**
   * Updates this state. Depending on the [changeType] the history is updated
   */
  fun update(newState: S, changeType: ChangeType): UndoState<S> {
    return update(Snapshot(newState, changeType, null))
  }

  /**
   * Updates the current state.
   *
   * Any update discards [future]: once the user edits again there is nothing left to redo.
   */
  fun update(newCurrent: Snapshot<S>): UndoState<S> {
    return when (newCurrent.changeType) {
      ChangeType.MinorChange -> {
        copy(
          current = newCurrent,
          previous = (previous + current).takeLast(maxHistoryLength),
          future = emptyList(),
        )
      }

      ChangeType.MajorChange -> {
        copy(
          current = newCurrent,
          previous = (previous + current).condenseMinor().takeLast(maxHistoryLength),
          future = emptyList(),
        )
      }

      ChangeType.HistoryReset -> {
        copy(
          current = newCurrent,
          previous = emptyList(),
          future = emptyList(),
        )
      }
    }
  }

  /**
   * Drops the intermediate minor snapshots from the history and turns the current one into a [ChangeType.MajorChange].
   *
   * Call this when a sequence of minor edits has come to an end (a text field loses focus, for example),
   * so that a later [undo] jumps back over the whole sequence instead of one keystroke at a time.
   */
  fun commitMinorChanges(): UndoState<S> {
    return when (current.changeType) {
      ChangeType.MinorChange -> {
        copy(
          previous = previous.condenseMinor().takeLast(maxHistoryLength),
          current = current.copy(changeType = ChangeType.MajorChange),
        )
      }

      ChangeType.MajorChange -> {
        copy(
          previous = previous.condenseMinor().takeLast(maxHistoryLength),
        )
      }

      ChangeType.HistoryReset -> {
        // nothing to do, history must already be empty
        require(previous.isEmpty()) { "History must be empty" }
        this
      }
    }
  }

  /**
   * Steps back to the newest snapshot of [previous] and moves the current one over to [future].
   *
   * Returns this state unchanged when there is nothing left to undo - callers do not have to check first.
   */
  fun undo(): UndoState<S> {
    return if (previous.isEmpty()) {
      this
    } else {
      this.copy(
        current = previous.last(),
        previous = previous.dropLast(1),
        future = buildList {
          add(current)
          addAll(future.take(maxHistoryLength))
        },
      )
    }
  }

  /**
   * Steps forward to the oldest snapshot of [future] - the one the most recent [undo] put there.
   *
   * Returns this state unchanged when there is nothing to redo - callers do not have to check first.
   */
  fun redo(): UndoState<S> {
    return if (future.isEmpty()) {
      this
    } else {
      this.copy(
        current = future.first(),
        previous = previous + current,
        future = future.drop(1),
      )
    }
  }

  /**
   * A history entry
   */
  data class Snapshot<S>(
    /**
     * The state
     */
    val state: S,

    /**
     * The type of change that resulted in this state
     */
    val changeType: ChangeType,

    /**
     * The optional label
     */
    val label: String?,
  )
}

/**
 * Drops the [ChangeType.MinorChange] snapshots.
 *
 * They are discarded, not merged into their successor: the states they held are gone from the history.
 */
fun <S> List<UndoState.Snapshot<S>>.condenseMinor(): List<UndoState.Snapshot<S>> {
  return this.filter {
    it.changeType != ChangeType.MinorChange
  }
}
