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
package it.neckar.ktor.jwt.jwks

import it.neckar.open.time.now
import it.neckar.open.unit.other.GuardedBy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Decides whether a token whose `kid` the held set does not know is worth an out-of-turn fetch.
 *
 * A `kid` nobody knows means one of two things: the realm rotated, or the token is not ours. This
 * planner refuses to decide that in the request — it only bounds how often the question may be put
 * to the realm.
 *
 * Three bounds, each of which alone keeps the load finite:
 * - **the window**: at most one planned refresh per [window], however many requests arrive;
 * - **the cap**: at most [maxRefreshesPerMinute] planned refreshes per minute and instance,
 *   whatever the window would allow;
 * - **the negative memory**: a `kid` a successful fetch did not deliver is not ours, and plans
 *   nothing further until the regular cycle renews the set.
 *
 * What it deliberately does not do is delay anything. The regular fetch cycle runs on its own
 * interval and never consults this planner, so a flood of unknown `kid`s can pull a refresh forward
 * and can never push one back.
 *
 * Thread-safe: every request touches it.
 */
class JwksRefreshPlanner(
  /**
   * The shortest distance between two planned refreshes. Together with the regular fetch interval it
   * brackets how long a rotation stays invisible: the interval is the upper bound, this the lower.
   */
  val window: Duration = DefaultWindow,
  /**
   * The hard ceiling per instance, independent of [window]. Bounds the load a flood of distinct
   * `kid`s can put on the realm even if every window is used.
   */
  val maxRefreshesPerMinute: Int = DefaultMaxRefreshesPerMinute,
) {
  private val lock = Any()

  @GuardedBy("lock")
  private var lastPlannedAt: Instant? = null

  /**
   * The instants of the planned refreshes still inside the trailing minute.
   */
  @GuardedBy("lock")
  private val plannedInTrailingMinute: ArrayDeque<Instant> = ArrayDeque()

  /**
   * The `kid`s a successful fetch established as not belonging to this realm. Bounded, oldest first:
   * a flood of distinct `kid`s must not grow the process's memory.
   */
  @GuardedBy("lock")
  private val absentKeyIds: LinkedHashSet<JwkKeyId> = LinkedHashSet()

  /**
   * The `kid`s asked for since the last successful fetch. Bounded the same way; [onFetchSucceeded]
   * turns the ones the fetch did not deliver into [absentKeyIds].
   */
  @GuardedBy("lock")
  private val askedSinceFetch: LinkedHashSet<JwkKeyId> = LinkedHashSet()

  /**
   * Whether an out-of-turn fetch should be triggered for [keyId] now. Records the decision, so two
   * calls in the same window answer `true` at most once.
   *
   * Never blocks and never fetches — the caller triggers the fetch and rejects the request either
   * way.
   */
  fun planRefreshFor(keyId: JwkKeyId): Boolean = synchronized(lock) {
    val now = now()

    if (keyId in absentKeyIds) {
      return@synchronized false
    }

    //After the absent check: a kid already answered would otherwise be re-answered on every request
    //and push the kids still waiting for an answer out of the bounded memory.
    askedSinceFetch.remember(keyId)

    lastPlannedAt?.let { last ->
      if (now - last < window) {
        return@synchronized false
      }
    }

    while (plannedInTrailingMinute.isNotEmpty() && now - plannedInTrailingMinute.first() >= 1.minutes) {
      plannedInTrailingMinute.removeFirst()
    }
    if (plannedInTrailingMinute.size >= maxRefreshesPerMinute) {
      return@synchronized false
    }

    lastPlannedAt = now
    plannedInTrailingMinute.addLast(now)
    true
  }

  /**
   * Answers the open question after a successful fetch: every `kid` asked for since the previous
   * fetch that [deliveredKeyIds] does not contain belongs to somebody else and is remembered as such.
   */
  fun onFetchSucceeded(deliveredKeyIds: Set<JwkKeyId>) {
    synchronized(lock) {
      askedSinceFetch.forEach { asked ->
        if (asked !in deliveredKeyIds) {
          absentKeyIds.remember(asked)
        }
      }
      askedSinceFetch.clear()
    }
  }

  /**
   * Forgets which `kid`s were established as absent. Called by the regular cycle: the set it just
   * renewed may hold keys that were not there when the memory was written.
   */
  fun forgetAbsentKeyIds() {
    synchronized(lock) {
      absentKeyIds.clear()
    }
  }

  /**
   * The `kid`s currently established as not belonging to this realm. For tests and diagnosis.
   */
  val knownAbsentKeyIds: Set<JwkKeyId>
    get() = synchronized(lock) { absentKeyIds.toSet() }

  private fun LinkedHashSet<JwkKeyId>.remember(keyId: JwkKeyId) {
    remove(keyId)
    add(keyId)
    while (size > MaxRememberedKeyIds) {
      remove(first())
    }
  }

  companion object {
    val DefaultWindow: Duration = 30.seconds
    const val DefaultMaxRefreshesPerMinute: Int = 4

    /**
     * How many `kid`s the two memories hold at most. Reached only under a flood, where every entry
     * beyond the newest few is worthless anyway.
     */
    const val MaxRememberedKeyIds: Int = 1024
  }
}
