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
package it.neckar.open.workflow

/**
 * Per-transition availability information for a workflow state.
 *
 * Three categories cover every possible outcome for a transition:
 *
 * - [Possible] — the transition can be taken right now. UI renders the action as enabled.
 * - [Impossible] — the target state is structurally reachable from the current state,
 *   but one or more preconditions are not met. Concrete implementations carry the
 *   specific reasons (boolean flags or data) so the frontend can show them as blockers.
 *   UI renders the action as disabled with the blocker list.
 * - [Unreachable] — the target state is not reachable from the current state at all
 *   (wrong source state, terminal state, structurally excluded). UI hides the action
 *   entirely (or renders it as unavailable without blocker list).
 *
 * Concrete per-transition types (one sealed interface per target state) implement
 * the subset of categories that actually applies to that transition — e.g. a transition
 * that is either always possible or never reachable omits [Impossible].
 *
 * See `internal/patterns/docs/kotlin/workflow-transitions.md` for the full pattern.
 */
interface TransitionInfo {
  val kind: Kind

  /**
   * The transition can be taken right now.
   */
  interface Possible : TransitionInfo {
    override val kind: Kind
      get() = Kind.Possible
  }

  /**
   * The target state is reachable, but preconditions are missing.
   *
   * Concrete implementations carry the specific reasons (boolean flags or data).
   */
  interface Impossible : TransitionInfo {
    override val kind: Kind
      get() = Kind.Impossible
  }

  /**
   * The target state is not reachable from the current state.
   */
  interface Unreachable : TransitionInfo {
    override val kind: Kind
      get() = Kind.Unreachable
  }

  /**
   * Discriminator for the three transition categories.
   */
  enum class Kind {
    Possible,
    Impossible,
    Unreachable,
  }
}
