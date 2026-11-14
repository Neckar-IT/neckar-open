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
package it.neckar.open.concurrent.repeating

import kotlin.time.Duration

/**
 * How long a [Repeating] job waits after a failed cycle, on top of its schedule, and how long [retrying] waits
 * between two attempts.
 *
 * Without one, a job whose dependency is down retries at full rate: a 15-second scheduler hammers a
 * dead GitLab 240 times an hour, fills the log with the same stack trace and delays its own recovery
 * by competing with it. With one, the retries thin out while the failures last and snap back to the
 * normal rhythm the moment a cycle completes.
 *
 * Opt-in: a job whose cycles are independent attempts (a poll that simply has nothing to report) wants
 * no backoff at all, which is the default.
 */
public interface ErrorBackoff {
  /**
   * How long to wait after [consecutiveFailures] failed cycles or attempts in a row. Called with `1` for the
   * first failure after a successful cycle, and after the first failed attempt of [retrying].
   */
  public fun delayAfter(consecutiveFailures: Int): Duration

  /**
   * The longest [delayAfter] ever returns. Part of the longest pause of a job's loop, which a
   * watchdog must not mistake for a dead loop.
   */
  public val maximumDelay: Duration

  public companion object {
    /**
     * Doubles the wait with every consecutive failure — [initial], `2 × initial`, `4 × initial`, … —
     * up to [maximum].
     *
     * The shape to reach for when the failure is likely transient and shared: every instance of a
     * service backs off together, so an overloaded dependency gets room to recover instead of a
     * retry storm at a fixed rate.
     *
     * ```
     * Repeating.every(15.seconds) { reconcile() }
     *   .errorBackoff(ErrorBackoff.exponential(initial = 30.seconds, maximum = 10.minutes))
     * ```
     */
    public fun exponential(initial: Duration, maximum: Duration): ErrorBackoff {
      require(initial.isPositive()) { "initial must be positive but was $initial" }
      require(maximum >= initial) { "maximum $maximum must not be shorter than initial $initial" }

      return object : ErrorBackoff {
        override fun delayAfter(consecutiveFailures: Int): Duration {
          //Cap the exponent before the multiplication: doubling 30 seconds 60 times overflows into a
          //meaningless Duration, and every exponent past the cap yields the maximum anyway.
          val doublings = (consecutiveFailures - 1).coerceIn(0, MaxDoublings)
          return (initial * (1 shl doublings)).coerceAtMost(maximum)
        }

        override val maximumDelay: Duration = maximum
      }
    }

    /**
     * Waits the same [delay] after every failed cycle or attempt, however many there were.
     */
    public fun constant(delay: Duration): ErrorBackoff {
      require(delay.isPositive()) { "delay must be positive but was $delay" }
      return object : ErrorBackoff {
        override fun delayAfter(consecutiveFailures: Int): Duration = delay

        override val maximumDelay: Duration = delay
      }
    }

    /**
     * How often [exponential] doubles before it stops: 2^30 × any sensible initial delay is far past
     * every maximum a caller would configure, and stopping there keeps the multiplication finite.
     */
    private const val MaxDoublings: Int = 30
  }
}
