/*
 * Copyright 2023 Neckar IT GmbH, Mössingen, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meistercharts.axis.time

import com.meistercharts.axis.time.DistanceDays.TicksPerMonth.EveryDay
import it.neckar.datetime.minimal.DayOfMonth
import it.neckar.datetime.minimal.LocalDate
import it.neckar.datetime.minimal.LocalDateTime
import it.neckar.datetime.minimal.Month
import it.neckar.datetime.minimal.TimeConstants
import it.neckar.datetime.minimal.TimeZone
import it.neckar.datetime.minimal.Year
import it.neckar.datetime.minimal.fromMillisCurrentTimeZone
import it.neckar.datetime.minimal.toMillis
import it.neckar.datetime.minimal.toMillisAtStartOfDay
import it.neckar.open.annotations.AllocationCost
import it.neckar.open.annotations.Allocates
import it.neckar.open.annotations.Hot
import it.neckar.open.collections.DoubleArrayList
import it.neckar.open.collections.emptyIntArray
import it.neckar.open.collections.fastForEach
import it.neckar.open.collections.fastForEachReversed
import it.neckar.open.collections.first
import it.neckar.open.formatting.dateFormat
import it.neckar.open.formatting.dateTimeFormat
import it.neckar.open.formatting.dateTimeFormatShort
import it.neckar.open.formatting.dateTimeFormatShortWithMillis
import it.neckar.open.formatting.yearMonthFormat
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.floor
import it.neckar.open.kotlin.lang.log10
import it.neckar.open.kotlin.lang.roundDownToBase
import it.neckar.open.time.formatUtcForDebug
import it.neckar.open.unit.other.Inclusive
import it.neckar.open.unit.si.ms
import it.neckar.open.unit.si.s
import it.neckar.open.unit.time.a
import it.neckar.open.unit.time.min
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.ulp
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Every computation runs in constant time per tick, within [TimeTickDistance.MinSupportedMillis] to [TimeTickDistance.MaxSupportedMillis].
 */
sealed interface TimeTickDistance : Comparable<TimeTickDistance> {
  /**
   * Calculates the tick values from the given start until the end
   */
  @Allocates(AllocationCost.Linear)
  fun calculateTicks(
    start: @ms @Inclusive Double,
    end: @ms @Inclusive Double,
    timeZone: TimeZone,
  ): @ms DoubleArrayList {
    require(start < end) { "Start <${start.formatUtcForDebug()}> must be smaller than end <${end.formatUtcForDebug()}>" }
    require(start >= MinSupportedMillis && end <= MaxSupportedMillis) {
      "Ticks between $start and $end leave the supported range $MinSupportedMillis..$MaxSupportedMillis"
    }
    val estimatedTicks = (end - start) / shortestMeanDistance()
    require(estimatedTicks <= MaxTicks) {
      "$estimatedTicks ticks of $this between ${start.formatUtcForDebug()} and ${end.formatUtcForDebug()} exceed $MaxTicks"
    }

    val ticks = calculateTicksInternal(start, end, timeZone)

    //Plausibility check
    if (ticks.isNotEmpty()) {
      @ms val firstTick: @ms Double = ticks.first()

      require(firstTick.isAtOrBefore(start)) {
        "First tick (${firstTick.formatUtcForDebug()}) must be before/same as start (${start.formatUtcForDebug()}) for $this. (start: ${start.formatUtcForDebug()} - end: ${end.formatUtcForDebug()}"
      }
    }

    return ticks
  }

  /**
   * Calculates the tick values - without any checks
   */
  @Allocates(AllocationCost.Linear)
  fun calculateTicksInternal(
    start: @Inclusive @ms Double,
    end: @Inclusive @ms Double,
    timeZone: TimeZone,
  ): @ms DoubleArrayList

  /**
   * Formats the given millis as offset
   */
  @Allocates(AllocationCost.Constant)
  fun formatAsOffset(millis: @ms Double, i18nConfiguration: I18nConfiguration): String

  /**
   * Returns a "global" index for the tick described by the given millis.
   * The index can then be used to select alternating colors (using modulo)
   *
   * Attention: The returned value is *guess*! It should not be used to calculate real values at all.
   */
  @Allocates(AllocationCost.Constant)
  fun calculateEstimatedIndex(millis: @ms Double, timeZone: TimeZone): GlobalTimeIndex

  /**
   * Returns the smallest possible tick distance for this *offset* distance
   * @return null if all tick distances are supported
   */
  @Hot
  fun smallestPossibleTickDistance(): TimeTickDistance

  /**
   * Compares two tick distances.
   * * First criteria: [timeMagnitude].
   * * Second criteria: [typicalDistanceForComparison]
   */
  override operator fun compareTo(other: TimeTickDistance): Int {
    return when {
      timeMagnitude != other.timeMagnitude -> {
        timeMagnitude.compareTo(other.timeMagnitude)
      }

      else -> {
        typicalDistanceForComparison().compareTo(other.typicalDistanceForComparison())
      }
    }
  }

  /**
   * Returns the distance between two ticks in millis.
   * This method is (only) used in [compareTo]
   */
  fun typicalDistanceForComparison(): @ms Double

  /**
   * Returns the time magnitude.
   * This method is (only) used in [compareTo]
   */
  val timeMagnitude: TimeMagnitude

  companion object {
    /**
     * The largest number of ticks [calculateTicks] computes; a smaller distance for the same span throws.
     */
    const val MaxTicks: Int = 10_000

    /**
     * The first day of `Year.Min` plus one day, so that every time zone offset keeps the start inside `LocalDate`.
     */
    val MinSupportedMillis: @ms Double = LocalDate.MinMillis + TimeConstants.MillisPerDay

    /**
     * One day before `Year.Max` starts: the ticks of months and days step up to one distance past the end and stay inside `LocalDate`.
     */
    val MaxSupportedMillis: @ms Double = LocalDate(Year.Max, Month.January, DayOfMonth.FirstDay).toEpochDay() * TimeConstants.MillisPerDay - TimeConstants.MillisPerDay

    /**
     * Returns the ideal time tick distance for the given min tick distance - use when calculating ticks
     */
    @Allocates(AllocationCost.Constant)
    fun forTicks(minTickDistance: @ms Double): TimeTickDistance {
      require(minTickDistance >= 0.0) { "min tick distance must be greater than or equal to 0 but was <$minTickDistance>" }

      return when {
        minTickDistance > 365.days.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceYears.atLeast(minTickDistance, Factors.All)
        }

        minTickDistance > 168.days.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceYears(1)
        }

        minTickDistance > 15.days.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceMonths.atLeast(minTickDistance)
        }

        minTickDistance > 12.hours.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceDays.atLeast(minTickDistance)
        }

        minTickDistance > 30.minutes.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceHours.atLeast(minTickDistance)
        }

        minTickDistance > 1.minutes.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceMinutes.atLeast(minTickDistance)
        }

        minTickDistance > 500.0 -> {
          DistanceSeconds.atLeast(minTickDistance)
        }

        else -> {
          /**
           * If the min tick distance is smaller than 500 millis, find the smaller magnitude
           */
          DistanceMillis.atLeast(minTickDistance, Factors.All)
        }
      }
    }

    /**
     * Returns the ideal time tick distance when calculating ticks for offsets
     */
    @Allocates(AllocationCost.Constant)
    fun forOffsets(minTickDistance: @ms Double): TimeTickDistance {
      require(minTickDistance >= 0.0) { "min tick distance must be greater than or equal to 0 but was <$minTickDistance>" }

      return when {
        minTickDistance > 60.days.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceYears.atLeast(minTickDistance, Factors.Only10s)
        }

        minTickDistance > 2.days.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceMonths(1)
        }

        minTickDistance > 2.hours.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceDays(EveryDay)
        }

        minTickDistance > 2.minutes.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceHours(1)
        }

        minTickDistance > 2.seconds.toDouble(DurationUnit.MILLISECONDS) -> {
          DistanceMinutes(1)
        }

        minTickDistance > 500.0 -> {
          DistanceSeconds(1)
        }

        else -> {
          /**
           * If the min tick distance is smaller than 500 millis, find the smaller magnitude
           */
          DistanceMillis.atLeast(minTickDistance, Factors.Only10s)
        }
      }
    }
  }

  /**
   * Used to compare two [TimeTickDistance] instances
   */
  enum class TimeMagnitude {
    Millis,
    Seconds,
    Minutes,
    Hours,
    Days,
    Months,
    Years,
  }
}

/**
 * Distance in years; [yearNumbering] decides which years carry a tick.
 */
class DistanceYears(
  val distanceInYears: @a Long,
  val yearNumbering: YearNumbering = YearNumbering.Astronomical,
) : TimeTickDistance {
  constructor(distanceInYears: @a Int) : this(distanceInYears.toLong())

  init {
    require(distanceInYears in 1..MaxDistanceInYears) { "distanceInYears <$distanceInYears> outside 1..$MaxDistanceInYears" }
  }

  override val timeMagnitude: TimeTickDistance.TimeMagnitude = TimeTickDistance.TimeMagnitude.Years

  override fun typicalDistanceForComparison(): @ms Double {
    return distanceInYears * TimeConstants.MillisPerYear
  }

  override fun calculateTicksInternal(start: Double, end: Double, timeZone: TimeZone): DoubleArrayList {
    val startYear = LocalDate.fromMillisCurrentTimeZone(start, timeZone).year.value
    val endYear = LocalDate.fromMillisCurrentTimeZone(end, timeZone).year.value + 1

    return DoubleArrayList().also {
      var current = yearNumbering.tickAtOrBefore(startYear, distanceInYears)
      if (current < Year.MinValue) {
        // The first year of LocalDate stands in for the tick before it; the next tick lies on the grid again.
        it.add(LocalDate(Year.Min, Month.January, DayOfMonth.FirstDay).toMillisAtStartOfDay(timeZone))
        current = yearNumbering.nextTick(current, distanceInYears)
      }

      while (current <= endYear) {
        it.add(LocalDate(Year(current), Month.January, DayOfMonth.FirstDay).toMillisAtStartOfDay(timeZone))
        current = yearNumbering.nextTick(current, distanceInYears)
      }
    }
  }

  override fun formatAsOffset(millis: @ms Double, i18nConfiguration: I18nConfiguration): String {
    val localDate = LocalDate.fromMillisCurrentTimeZone(millis, i18nConfiguration.timeZone)

    // The first year of LocalDate stands in for a tick before it.
    return yearNumbering.tickAtOrBefore(localDate.year.value, distanceInYears).coerceAtLeast(Year.MinValue).toString()
  }

  override fun calculateEstimatedIndex(millis: @ms Double, timeZone: TimeZone): GlobalTimeIndex {
    val localDate = LocalDate.fromMillisCurrentTimeZone(millis, timeZone)
    return GlobalTimeIndex(yearNumbering.tickIndex(localDate.year.value, distanceInYears).toGlobalIndexValue())
  }

  @Hot
  override fun smallestPossibleTickDistance(): TimeTickDistance {
    return DistanceDays(EveryDay)
  }

  /**
   * This distance with ticks at the years of [yearNumbering]
   */
  fun withYearNumbering(yearNumbering: YearNumbering): DistanceYears {
    return DistanceYears(distanceInYears, yearNumbering)
  }

  override fun toString(): String {
    return "DistanceYears($distanceInYears, $yearNumbering)"
  }

  companion object {
    fun atLeast(minTickDistance: @ms Double, factors: Factors): DistanceYears {
      @min val minTickDistanceInYears = minTickDistance / 365.days.toDouble(DurationUnit.MILLISECONDS)
      val log10 = minTickDistanceInYears.log10().floor()
      val magnitude = 10.0.pow(log10)
      val factor = factors.calculateFactor(minTickDistanceInYears / magnitude)
      val tickDistanceYears = magnitude * factor
      return DistanceYears(ceil(tickDistanceYears).toLong().coerceAtMost(MaxDistanceInYears))
    }

    /**
     * Ten billion years: the widest window of 30 billion years spans 3 ticks.
     */
    const val MaxDistanceInYears: @a Long = 10_000_000_000L

    val OneYear: DistanceYears = DistanceYears(1)
  }
}

/**
 * Distance in months
 */
class DistanceMonths(val distanceInMonths: Int) : TimeTickDistance {
  init {
    require(distanceInMonths in 1..11) { "invalid distanceInMonths: $distanceInMonths" }
  }

  override val timeMagnitude: TimeTickDistance.TimeMagnitude = TimeTickDistance.TimeMagnitude.Months

  override fun typicalDistanceForComparison(): @ms Double {
    return distanceInMonths * TimeConstants.MillisPerDay * 30
  }

  override fun calculateTicksInternal(start: Double, end: Double, timeZone: TimeZone): DoubleArrayList {
    val startLocalDateExact = LocalDate.fromMillisCurrentTimeZone(start, timeZone).atStartOfMonth()
    val endLocalDate = LocalDate.fromMillisCurrentTimeZone(end, timeZone).atStartOfNextMonth()

    val startLocalDate = startLocalDateExact.withMonth(
      Month((startLocalDateExact.month.value - 1).roundDownToBase(distanceInMonths).plus(1))
    )

    return DoubleArrayList().also {
      var current = startLocalDate

      while (current <= endLocalDate) {
        it.add(current.toMillisAtStartOfDay(timeZone))
        current = current.plusMonths(distanceInMonths.toLong())
      }
    }
  }

  override fun formatAsOffset(millis: Double, i18nConfiguration: I18nConfiguration): String {
    return yearMonthFormat.format(millis, i18nConfiguration)
  }

  override fun calculateEstimatedIndex(millis: Double, timeZone: TimeZone): GlobalTimeIndex {
    val localDate = LocalDate.fromMillisCurrentTimeZone(millis, timeZone)
    return GlobalTimeIndex(localDate.monthOfEpoche().floorDiv(distanceInMonths.toLong()).toGlobalIndexValue())
  }

  @Hot
  override fun smallestPossibleTickDistance(): TimeTickDistance {
    return DistanceDays(EveryDay)
  }

  override fun toString(): String {
    return "DistanceMonths($distanceInMonths)"
  }

  companion object {
    private val possibleValues = intArrayOf(1, 2, 3, 6)

    fun atLeast(minTickDistance: @ms Double): DistanceMonths {
      @min val minTickDistanceInMonths = minTickDistance / 28.days.toDouble(DurationUnit.MILLISECONDS)
      @min val tickDistanceInMonths = possibleValues.first { it >= minTickDistanceInMonths }

      return DistanceMonths(tickDistanceInMonths)
    }
  }
}

/**
 * Distance in days
 */
class DistanceDays(val ticksPerMonth: TicksPerMonth) : TimeTickDistance {
  override val timeMagnitude: TimeTickDistance.TimeMagnitude = TimeTickDistance.TimeMagnitude.Days

  override fun typicalDistanceForComparison(): @ms Double {
    return ticksPerMonth.worstCaseDaysDistance() * TimeConstants.MillisPerDay
  }

  /**
   * Contains the possible distances for days
   */
  enum class TicksPerMonth(
    /**
     * Contains the potential days.
     * Attention: Is left empty for [EveryDay].
     */
    val days: IntArray,
  ) {
    EveryDay(emptyIntArray()),
    Every5Days(intArrayOf(1, 5, 10, 15, 20, 25)),
    Every10Days(intArrayOf(1, 10, 20)),
    Every15Days(intArrayOf(1, 15)),
    ;

    /**
     * Returns the distance between two ticks in days - in the worst case
     */
    fun worstCaseDaysDistance(): Int {
      return when (this) {
        EveryDay -> 1
        Every5Days -> 7 //25 --> 32
        Every10Days -> 12 //20 --> 32
        Every15Days -> 17 //15 --> 32
      }
    }

    fun typicalCaseDaysDistance(): Int {
      return when (this) {
        EveryDay -> 1
        Every5Days -> 5
        Every10Days -> 10
        Every15Days -> 15
      }
    }

    /**
     * Returns the same day - or the one below for this enum option
     */
    fun sameOrBelow(day: DayOfMonth): DayOfMonth {
      when (this) {
        EveryDay -> return day
        Every5Days, Every10Days, Every15Days -> {
          days.fastForEachReversed { potentialDayAsInt ->
            if (potentialDayAsInt <= day.value) {
              return DayOfMonth(potentialDayAsInt)
            }
          }
        }
      }
      throw IllegalStateException("No valid day found! for $day")
    }

    /**
     * Iterates from start to end. Calls the callback for each steap
     */
    inline fun iterate(start: @Inclusive LocalDate, end: @Inclusive LocalDate, callback: (LocalDate) -> Unit) {
      var current = start

      while (current <= end) {
        callback(current)

        current = calculateNext(current)
      }
    }

    /**
     * Calculates the next local date
     */
    fun calculateNext(current: @Inclusive LocalDate): @Inclusive LocalDate {
      return when (this) {
        EveryDay -> current.plusDays(1)
        Every5Days -> {
          val nextDayValue = days.firstOrNull { it > current.dayOfMonth.value } ?: return current.atStartOfNextMonth()
          current.atDayOfMonth(DayOfMonth(nextDayValue))
        }

        Every10Days, Every15Days -> {
          val nextDayValue = days.firstOrNull { it > current.dayOfMonth.value } ?: return current.atStartOfNextMonth()
          current.atDayOfMonth(DayOfMonth(nextDayValue))
        }
      }
    }

    /**
     * Returns the ticks per month (estimation)
     */
    fun ticksPerMonth(): Int {
      return when (this) {
        EveryDay -> 31
        else -> days.size
      }
    }

    companion object {
      fun forMinDistance(minTickDistance: @ms Double): TicksPerMonth {
        @min val minTickDistanceInDays = minTickDistance / 24.hours.toDouble(DurationUnit.MILLISECONDS)
        @min val tickDistanceInDays = possibleValues.first { it >= minTickDistanceInDays }

        return when {
          tickDistanceInDays >= 15 -> Every15Days
          tickDistanceInDays >= 10 -> Every10Days
          tickDistanceInDays >= 5 -> Every5Days
          else -> EveryDay
        }
      }
    }
  }

  override fun calculateTicksInternal(start: Double, end: Double, timeZone: TimeZone): DoubleArrayList {
    val startLocalDateExact = LocalDate.fromMillisCurrentTimeZone(start, timeZone)
    val endLocalDate = LocalDate.fromMillisCurrentTimeZone(end, timeZone).plusDays(ticksPerMonth.worstCaseDaysDistance().toLong())

    val startLocalDate = startLocalDateExact.withDay(
      ticksPerMonth.sameOrBelow(startLocalDateExact.dayOfMonth)
    )

    return DoubleArrayList().also { list ->
      ticksPerMonth.iterate(startLocalDate, endLocalDate) {
        list.add(it.toMillisAtStartOfDay(timeZone))
      }
    }
  }

  override fun formatAsOffset(millis: Double, i18nConfiguration: I18nConfiguration): String {
    return dateFormat.format(millis, i18nConfiguration)
  }

  override fun calculateEstimatedIndex(millis: @ms Double, timeZone: TimeZone): GlobalTimeIndex {
    val localDate = LocalDate.fromMillisCurrentTimeZone(millis, timeZone)
    val monthOfEpoche = localDate.monthOfEpoche()
    val dayOfMonth = localDate.dayOfMonth

    val tickOfEpoche = monthOfEpoche * ticksPerMonth.ticksPerMonth() + dayOfMonth.value / ticksPerMonth.typicalCaseDaysDistance()

    return GlobalTimeIndex(tickOfEpoche.toGlobalIndexValue())
  }

  @Hot
  override fun smallestPossibleTickDistance(): TimeTickDistance {
    return DistanceMinutes(1)
  }

  override fun toString(): String {
    return "DistanceDays($ticksPerMonth)"
  }

  companion object {
    private val possibleValues = intArrayOf(1, 5, 10, 15)

    fun atLeast(minTickDistance: @ms Double): DistanceDays {
      return DistanceDays(TicksPerMonth.forMinDistance(minTickDistance))
    }
  }
}

/**
 * Distance in hours.
 * ATTENTION: There are timezones with 30 minute offset. Therefore, it is necessary to use [LocalDate] here, too.
 */
class DistanceHours(val distanceInHours: Int) : TimeTickDistance {
  override val timeMagnitude: TimeTickDistance.TimeMagnitude = TimeTickDistance.TimeMagnitude.Hours

  override fun typicalDistanceForComparison(): @ms Double {
    return distanceInHours * TimeConstants.MillisPerHour
  }

  override fun calculateTicksInternal(start: @Inclusive @ms Double, end: @Inclusive @ms Double, timeZone: TimeZone): DoubleArrayList {
    val startLocalDateTimeExact = LocalDateTime.fromMillisCurrentTimeZone(start, timeZone)

    //The local hour of day
    val hoursOfDay = startLocalDateTimeExact.hour
    val startHour = hoursOfDay.roundDownToBase(distanceInHours)

    val startLocalDateTime = startLocalDateTimeExact.atStartOfHour(startHour)
    @ms val startMillis = startLocalDateTime.toMillis(timeZone)
    return fixedStepTicks(startMillis, end, distanceInHours * TimeConstants.MillisPerHour)
  }

  override fun formatAsOffset(millis: Double, i18nConfiguration: I18nConfiguration): String {
    return dateTimeFormatShort.format(millis, i18nConfiguration)
  }

  override fun calculateEstimatedIndex(millis: Double, timeZone: TimeZone): GlobalTimeIndex {
    return GlobalTimeIndex(floor(millis / TimeConstants.MillisPerHour).toLong().toGlobalIndexValue())
  }

  @Hot
  override fun smallestPossibleTickDistance(): TimeTickDistance {
    return DistanceMillis.smallest
  }

  override fun toString(): String {
    return "DistanceHours(hours=$distanceInHours)"
  }

  companion object {
    private val possibleValues = intArrayOf(1, 2, 3, 4, 6, 12)

    fun atLeast(minTickDistance: @ms Double): DistanceHours {
      @min val minTickDistanceInHours = minTickDistance / 1.hours.toDouble(DurationUnit.MILLISECONDS)
      @min val tickDistanceInHours = possibleValues.first { it >= minTickDistanceInHours }

      return DistanceHours(tickDistanceInHours)
    }
  }
}

/**
 * Distance in minutes
 */
class DistanceMinutes(val distanceInMinutes: @min Int) : TimeTickDistance {
  override val timeMagnitude: TimeTickDistance.TimeMagnitude = TimeTickDistance.TimeMagnitude.Minutes

  override fun typicalDistanceForComparison(): @ms Double {
    return distanceInMinutes * TimeConstants.MillisPerMinute
  }

  override fun calculateTicksInternal(start: Double, end: Double, timeZone: TimeZone): DoubleArrayList {
    @ms val step = distanceInMinutes * TimeConstants.MillisPerMinute
    return fixedStepTicks(start.roundDownToBase(step), end, step)
  }

  override fun formatAsOffset(millis: Double, i18nConfiguration: I18nConfiguration): String {
    return dateTimeFormatShort.format(millis, i18nConfiguration)
  }

  override fun calculateEstimatedIndex(millis: Double, timeZone: TimeZone): GlobalTimeIndex {
    @min val minuteOfEpoch = floor(millis / TimeConstants.MillisPerMinute).toLong()
    return GlobalTimeIndex(minuteOfEpoch.floorDiv(distanceInMinutes.toLong()).toGlobalIndexValue())
  }

  @Hot
  override fun smallestPossibleTickDistance(): TimeTickDistance {
    return DistanceMillis.smallest
  }

  override fun toString(): String {
    return "DistanceMinutes($distanceInMinutes)"
  }

  companion object {
    private val possibleValues: IntArray = intArrayOf(1, 2, 5, 10, 15, 30)

    fun atLeast(minTickDistance: @ms Double): DistanceMinutes {
      @min val minTickDistanceInMinutes = minTickDistance / 1.minutes.toDouble(DurationUnit.MILLISECONDS)
      @min val tickDistanceInMinutes = possibleValues.first { it >= minTickDistanceInMinutes }

      return DistanceMinutes(tickDistanceInMinutes)
    }
  }
}

/**
 * Distance in seconds
 */
class DistanceSeconds(val distanceInSeconds: @s Int) : TimeTickDistance {
  override val timeMagnitude: TimeTickDistance.TimeMagnitude = TimeTickDistance.TimeMagnitude.Seconds

  override fun typicalDistanceForComparison(): @ms Double {
    return distanceInSeconds * TimeConstants.MillisPerSecond
  }

  override fun calculateTicksInternal(start: Double, end: Double, timeZone: TimeZone): DoubleArrayList {
    @s val startSecondOfEpoch = (start / TimeConstants.MillisPerSecond).roundDownToBase(distanceInSeconds.toDouble())
    return fixedStepTicks(startSecondOfEpoch * TimeConstants.MillisPerSecond, end, distanceInSeconds * TimeConstants.MillisPerSecond)
  }

  override fun formatAsOffset(millis: Double, i18nConfiguration: I18nConfiguration): String {
    return dateTimeFormat.format(millis, i18nConfiguration)
  }

  override fun calculateEstimatedIndex(millis: Double, timeZone: TimeZone): GlobalTimeIndex {
    @s val secondOfEpoch = floor(millis / TimeConstants.MillisPerSecond).toLong()
    return GlobalTimeIndex(secondOfEpoch.floorDiv(distanceInSeconds.toLong()).toGlobalIndexValue())
  }

  @Hot
  override fun smallestPossibleTickDistance(): TimeTickDistance {
    return DistanceMillis.smallest
  }

  override fun toString(): String {
    return "DistanceSeconds($distanceInSeconds)"
  }

  companion object {
    private val possibleValues: IntArray = intArrayOf(1, 2, 5, 10, 15, 30, 60)

    fun atLeast(minTickDistance: @ms Double): DistanceSeconds {
      @s val minTickDistanceInSeconds = minTickDistance / 1.seconds.toDouble(DurationUnit.MILLISECONDS)
      @s val tickDistanceInSeconds = possibleValues.first { it >= minTickDistanceInSeconds }

      return DistanceSeconds(tickDistanceInSeconds)
    }
  }
}

/**
 * A time tick distance in millis
 */
class DistanceMillis(val distanceInMillis: @ms Double) : TimeTickDistance {
  init {
    require(distanceInMillis >= 0.0) { "distanceInMillis must be >= 0 but was <$distanceInMillis>" }
  }

  override val timeMagnitude: TimeTickDistance.TimeMagnitude = TimeTickDistance.TimeMagnitude.Millis

  override fun typicalDistanceForComparison(): @ms Double {
    return distanceInMillis
  }

  override fun calculateTicksInternal(start: Double, end: Double, timeZone: TimeZone): DoubleArrayList {
    return fixedStepTicks(start.roundDownToBase(distanceInMillis), end, distanceInMillis)
  }

  override fun formatAsOffset(millis: Double, i18nConfiguration: I18nConfiguration): String {
    return dateTimeFormatShortWithMillis.format(millis, i18nConfiguration)
  }

  override fun calculateEstimatedIndex(millis: Double, timeZone: TimeZone): GlobalTimeIndex {
    return GlobalTimeIndex(floor(millis / distanceInMillis).toLong().toGlobalIndexValue())
  }

  @Hot
  override fun smallestPossibleTickDistance(): TimeTickDistance {
    return smallest
  }

  override fun toString(): String {
    return "DistanceMillis($distanceInMillis)"
  }

  companion object {
    val smallest: DistanceMillis = DistanceMillis(1.0)

    fun atLeast(minTickDistance: @ms Double, factors: Factors): DistanceMillis {
      val log10 = minTickDistance.log10().floor()
      val magnitude = 10.0.pow(log10)
      val factor = factors.calculateFactor(minTickDistance / magnitude)
      @ms val tickDistance = (magnitude * factor).coerceAtLeast(1.0)
      check(tickDistance >= minTickDistance) { "$tickDistance < $minTickDistance" }

      return DistanceMillis(tickDistance)
    }
  }

}


enum class Factors(val values: DoubleArray) {
  All(doubleArrayOf(2.0, 2.5, 5.0, 10.0)),
  Default(doubleArrayOf(2.0, 5.0, 10.0)),
  Only5s(doubleArrayOf(5.0, 10.0)),
  Only10s(doubleArrayOf(10.0));


  /**
   * Calculates a factor
   */
  fun calculateFactor(minValue: Double): Double {
    var factor = 1.0
    while (factor < minValue) {
      values.fastForEach {
        if (factor * it > minValue) {
          return factor * it
        }
      }
      factor *= values.last()
    }
    return factor
  }
}

/**
 * Whether this tick lies at or before [start]; subtracting and adding a time zone offset far from 1970 moves a tick by the resolution of `Double`.
 */
private fun Double.isAtOrBefore(start: @ms Double): Boolean {
  return this - start <= max(1.0, start.ulp)
}

/**
 * The mean distance of neighbouring ticks in the shortest months, so that [TimeTickDistance.MaxTicks] bounds every span.
 */
private fun TimeTickDistance.shortestMeanDistance(): @ms Double {
  return when (this) {
    is DistanceDays -> ShortestMonthMillis / ticksPerMonth.ticksPerMonth()
    is DistanceMonths -> ShortestMonthMillis * distanceInMonths
    is DistanceYears, is DistanceHours, is DistanceMinutes, is DistanceSeconds, is DistanceMillis -> typicalDistanceForComparison()
  }
}

private const val ShortestMonthMillis: @ms Double = 28 * TimeConstants.MillisPerDay

/**
 * Ticks at [firstTick] plus every multiple of [step] up to [end].
 *
 * Each tick is computed from its index: far from 1970 an addition below the resolution of `Double`
 * leaves the value unchanged, and equal neighbors are dropped.
 */
private fun fixedStepTicks(firstTick: @ms Double, end: @ms Double, step: @ms Double): @ms DoubleArrayList {
  require(step > 0.0) { "step must be > 0 but was <$step>" }
  return DoubleArrayList().also { list ->
    var index = 0L
    while (true) {
      @ms val tick = firstTick + index * step
      if (tick > end) {
        return@also
      }
      if (list.isEmpty() || list[list.size - 1] != tick) {
        list.add(tick)
      }
      index++
    }
  }
}

/**
 * The index within the range of `Int`; `rem` keeps the sign, so the indices below and above 0 stay neighbours.
 */
private fun Long.toGlobalIndexValue(): Int {
  return rem(Int.MAX_VALUE.toLong()).toInt()
}

/**
 * This distance with the ticks of [yearNumbering] if it is a [DistanceYears]
 */
fun TimeTickDistance.withYearNumbering(yearNumbering: YearNumbering): TimeTickDistance {
  return when (this) {
    is DistanceYears -> withYearNumbering(yearNumbering)
    is DistanceMonths, is DistanceDays, is DistanceHours, is DistanceMinutes, is DistanceSeconds, is DistanceMillis -> this
  }
}
