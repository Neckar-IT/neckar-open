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

import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.logging.info
import it.neckar.open.time.now
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinDuration
import kotlin.time.toKotlinInstant

/**
 * Runs a job at a wall-clock time instead of on an interval — "at :15 past every hour", "every night
 * at 03:00", "Thursdays at 22:00" — through the same [Repeating] machinery, so a scheduled job gets
 * the same per-cycle timeout, the same error handling and the same liveness reporting as every
 * interval job:
 *
 * ```
 * Repeating.dailyAt(LocalTime.of(3, 0), BerlinZone) { validateTokens() }
 *   .cycleTimeout(30.minutes)
 *   .trackedAs(BackgroundJobName("token-validation"), stalenessThreshold = 26.hours)
 *   .start(scope)
 * ```
 *
 * **Wall-clock, not elapsed time.** The next run is computed from the calendar in [zone] before every
 * cycle, so a daylight-saving change shifts the job with the local clock instead of leaving it an
 * hour off. That calendar arithmetic is `java.time`, hence JVM-only.
 *
 * **A slot passed during a cycle runs right after it**, once for several passed slots: on a daily
 * 04:30 schedule a triggered cycle from 04:20 to 04:50 lets the 04:30 cycle run at 04:50.
 *
 * **An error backoff adds to the schedule.** The next slot follows the end of the backoff; the slots
 * passed before do not run.
 *
 * **A slot runs once.** A wall clock stepped back after a cycle keeps the next slot after the slot of
 * that cycle, so the slots the step repeats do not run again.
 *
 * **A run missed while the job was down is not made up.** A process that was down at 03:00 runs the
 * job the next night, not at startup: these jobs are periodic housekeeping, and firing them late is
 * rarely more useful than firing them on time next round. A caller that wants to catch one up asks
 * [lastRunAtOrBefore].
 */
public abstract class WallClockSchedule(
  /**
   * The time zone the wall-clock time is read in. Daylight saving is applied by [ZonedDateTime].
   */
  public val zone: ZoneId,
) : CycleSchedule {
  /**
   * The next moment this schedule fires, strictly after [reference] — "strictly" so a job that just
   * ran at 03:00 does not immediately match 03:00 again.
   */
  public abstract fun nextRunAfter(reference: ZonedDateTime): ZonedDateTime

  /**
   * The latest moment this schedule fired at or before [reference] — at-or-before, so a slot falling
   * exactly on [reference] is the last run and [nextRunAfter] answers with the one after it.
   */
  public abstract fun lastRunAtOrBefore(reference: ZonedDateTime): ZonedDateTime

  final override suspend fun awaitCycle(after: Instant?): Instant {
    val currentTime = currentTime()
    val nextRun = nextRunAfter(after?.toZonedDateTime() ?: currentTime)

    if (nextRun.isAfter(currentTime).not()) {
      logger.info { "Run of [$description] at $nextRun and every later slot up to now have passed - running one cycle now" }
      //The current time instead of the slot, so the slots passed since collapse into this one cycle
      return currentTime.toKotlinInstant()
    }

    val sleepFor: Duration = java.time.Duration.between(currentTime, nextRun).toKotlinDuration()
    logger.info { "Next run of [$description] at $nextRun (sleeping for $sleepFor)" }
    delay(sleepFor)
    //A wake-up before the slot by the wall clock counts as the slot, a later one as the current time
    return maxOf(nextRun, currentTime()).toKotlinInstant()
  }

  /**
   * The current wall-clock time in [zone], read through the repo-wide (and therefore mockable)
   * `it.neckar.open.time.now()`. Private because a subclass is handed the reference time and never
   * reads the clock itself.
   */
  private fun currentTime(): ZonedDateTime = now().toZonedDateTime()

  private fun Instant.toZonedDateTime(): ZonedDateTime = ZonedDateTime.ofInstant(toJavaInstant(), zone)

  private fun ZonedDateTime.toKotlinInstant(): Instant = toInstant().toKotlinInstant()

  override fun toString(): String = description

  public companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.open.concurrent.WallClockSchedule")
  }
}

/**
 * Fires every hour at [minuteOfHour] past the hour in [zone].
 *
 * A pause other than zero spreads the load away from the full hour, where cron jobs, backups and
 * monitoring scrapes pile up.
 */
public class HourlySchedule(
  /**
   * The minute past the hour the job runs at, `0`..`59`.
   */
  public val minuteOfHour: Int,
  zone: ZoneId,
) : WallClockSchedule(zone) {
  init {
    require(minuteOfHour in 0..59) { "minuteOfHour must be 0..59 but was $minuteOfHour" }
  }

  override val description: String
    get() = "hourly at :${minuteOfHour.toString().padStart(2, '0')} $zone"

  override val maxGap: Duration
    get() = 1.hours

  override fun nextRunAfter(reference: ZonedDateTime): ZonedDateTime {
    val thisHour = reference.truncatedTo(ChronoUnit.HOURS).plusMinutes(minuteOfHour.toLong())
    return if (thisHour.isAfter(reference)) thisHour else thisHour.plusHours(1)
  }

  override fun lastRunAtOrBefore(reference: ZonedDateTime): ZonedDateTime {
    val thisHour = reference.truncatedTo(ChronoUnit.HOURS).plusMinutes(minuteOfHour.toLong())
    return if (thisHour.isAfter(reference)) thisHour.minusHours(1) else thisHour
  }
}

/**
 * Fires every day at [time] in [zone].
 */
public class DailySchedule(
  /**
   * The local time of day the job runs at.
   */
  public val time: LocalTime,
  zone: ZoneId,
) : WallClockSchedule(zone) {
  override val description: String
    get() = "daily at $time $zone"

  //The day a daylight-saving change adds an hour is 25 hours long
  override val maxGap: Duration
    get() = 25.hours

  override fun nextRunAfter(reference: ZonedDateTime): ZonedDateTime {
    val today = reference.toLocalDate().atTime(time).atZone(zone)
    return if (today.isAfter(reference)) today else reference.toLocalDate().plusDays(1).atTime(time).atZone(zone)
  }

  override fun lastRunAtOrBefore(reference: ZonedDateTime): ZonedDateTime {
    val today = reference.toLocalDate().atTime(time).atZone(zone)
    return if (today.isAfter(reference)) reference.toLocalDate().minusDays(1).atTime(time).atZone(zone) else today
  }
}

/**
 * Fires every week on [day] at [time] in [zone].
 */
public class WeeklySchedule(
  /**
   * The day of week the job runs on.
   */
  public val day: DayOfWeek,
  /**
   * The local time of day the job runs at.
   */
  public val time: LocalTime,
  zone: ZoneId,
) : WallClockSchedule(zone) {
  override val description: String
    get() = "weekly on $day at $time $zone"

  //A week containing the daylight-saving change that adds an hour
  override val maxGap: Duration
    get() = 7.days + 1.hours

  override fun nextRunAfter(reference: ZonedDateTime): ZonedDateTime {
    var candidate: LocalDateTime = reference.toLocalDate().atTime(time)

    repeat(MaxSearchDays) {
      if (candidate.dayOfWeek == day && candidate.atZone(zone).isAfter(reference)) {
        return candidate.atZone(zone)
      }
      candidate = candidate.plusDays(1)
    }

    throw IllegalStateException("no $day at $time found within $MaxSearchDays days after $reference")
  }

  override fun lastRunAtOrBefore(reference: ZonedDateTime): ZonedDateTime {
    var candidate: LocalDateTime = reference.toLocalDate().atTime(time)

    repeat(MaxSearchDays) {
      if (candidate.dayOfWeek == day && candidate.atZone(zone).isAfter(reference).not()) {
        return candidate.atZone(zone)
      }
      candidate = candidate.minusDays(1)
    }

    throw IllegalStateException("no $day at $time found within $MaxSearchDays days before $reference")
  }

  public companion object {
    /**
     * How far a search steps, in either direction. Eight days covers every weekday plus the day the
     * search started on.
     */
    internal const val MaxSearchDays: Int = 8
  }
}

/**
 * Fires Monday to Friday at [time] in [zone]; Saturday and Sunday have no slot.
 */
public class WeekdaySchedule(
  /**
   * The local time of day the job runs at.
   */
  public val time: LocalTime,
  zone: ZoneId,
) : WallClockSchedule(zone) {
  override val description: String
    get() = "weekdays at $time $zone"

  //Friday to Monday, plus the hour a daylight-saving change adds
  override val maxGap: Duration
    get() = 3.days + 1.hours

  override fun nextRunAfter(reference: ZonedDateTime): ZonedDateTime {
    var candidate: LocalDateTime = reference.toLocalDate().atTime(time)

    repeat(MaxSearchDays) {
      if (candidate.dayOfWeek.isWeekday() && candidate.atZone(zone).isAfter(reference)) {
        return candidate.atZone(zone)
      }
      candidate = candidate.plusDays(1)
    }

    throw IllegalStateException("no weekday at $time found within $MaxSearchDays days after $reference")
  }

  override fun lastRunAtOrBefore(reference: ZonedDateTime): ZonedDateTime {
    var candidate: LocalDateTime = reference.toLocalDate().atTime(time)

    repeat(MaxSearchDays) {
      if (candidate.dayOfWeek.isWeekday() && candidate.atZone(zone).isAfter(reference).not()) {
        return candidate.atZone(zone)
      }
      candidate = candidate.minusDays(1)
    }

    throw IllegalStateException("no weekday at $time found within $MaxSearchDays days before $reference")
  }

  private fun DayOfWeek.isWeekday(): Boolean = this != DayOfWeek.SATURDAY && this != DayOfWeek.SUNDAY

  public companion object {
    /**
     * How far a search steps, in either direction. Four days cover a weekend plus the day the search started on.
     */
    internal const val MaxSearchDays: Int = 4
  }
}

/**
 * Repeats [block] every hour at [minuteOfHour] past the hour in [zone]. See [WallClockSchedule].
 */
public fun Repeating.Companion.hourlyAt(
  minuteOfHour: Int,
  zone: ZoneId,
  block: suspend () -> Unit,
): Repeating.RepeatingJobDescriptor.Builder = following(HourlySchedule(minuteOfHour, zone), block)

/**
 * Repeats [block] every day at [time] in [zone]. See [WallClockSchedule].
 */
public fun Repeating.Companion.dailyAt(
  time: LocalTime,
  zone: ZoneId,
  block: suspend () -> Unit,
): Repeating.RepeatingJobDescriptor.Builder = following(DailySchedule(time, zone), block)

/**
 * Repeats [block] every week on [day] at [time] in [zone]. See [WallClockSchedule].
 */
public fun Repeating.Companion.weeklyAt(
  day: DayOfWeek,
  time: LocalTime,
  zone: ZoneId,
  block: suspend () -> Unit,
): Repeating.RepeatingJobDescriptor.Builder = following(WeeklySchedule(day, time, zone), block)
