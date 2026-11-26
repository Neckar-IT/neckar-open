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
package it.neckar.datetime.minimal

import it.neckar.datetime.minimal.io.LocalDateSerializer
import it.neckar.open.annotations.CrossLayer
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.unit.si.ms
import kotlinx.serialization.Serializable
import kotlin.math.floor

/**
 * A date in the proleptic gregorian calendar, from [Year.Min] to [Year.Max].
 *
 * Every computation runs in common code in constant time, so JVM, JS and WasmJS return the same
 * date for the same instant, also outside the range of `java.time`, `Date` and `Temporal`.
 */
@CrossLayer("Calendar date — a single ISO yyyy-MM-dd scalar that lives identically in Domain, Mongo Entity, and on the REST wire, like NativeInstant and NativeUuid. Serializes as a primitive string, so embedding it in a DataView exposes no business internals.")
@Serializable(with = LocalDateSerializer::class)
data class LocalDate(
  val year: Year,
  val month: Month,
  val dayOfMonth: DayOfMonth,
) : Comparable<LocalDate> {

  init {
    require(dayOfMonth.value in 1..month.daysInMonth(year).value) {
      "Invalid date: day ${dayOfMonth.value} outside 1..${month.daysInMonth(year).value} in $year-${month.value}"
    }
  }

  /**
   * Calculates the months since the start of the epoch
   */
  fun monthOfEpoche(): Long {
    return year.value * 12 + month.value
  }

  fun dayOfYear(): Int {
    return month.daysBeforeThisMonth(year) + dayOfMonth.value
  }

  /**
   * Days since 1970-01-01, algorithm `days_from_civil` by Howard Hinnant.
   */
  fun toEpochDay(): Long {
    val monthValue = month.value
    val shiftedYear = if (monthValue <= 2) year.value - 1 else year.value
    val era = shiftedYear.floorDiv(400L)
    val yearOfEra = shiftedYear - era * 400
    val dayOfShiftedYear = dayOfShiftedYear(monthValue, dayOfMonth.value)
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfShiftedYear
    return era * DaysPerGregorianEra + dayOfEra - GregorianEpochShift
  }

  /**
   * ISO 8601 with expanded year like `java.time`: `-0043-03-15`, `1789-07-14`, `+10000-01-01`.
   */
  fun format(): String {
    val yearValue = year.value
    val absoluteYear = if (yearValue < 0) -yearValue else yearValue
    val paddedYear = absoluteYear.toString().padStart(4, '0')
    val formattedYear = when {
      yearValue < 0 -> "-$paddedYear"
      yearValue > 9999 -> "+$paddedYear"
      else -> paddedYear
    }
    val formattedMonth = month.value.toString().padStart(2, '0')
    val formattedDay = dayOfMonth.value.toString().padStart(2, '0')

    return "$formattedYear-$formattedMonth-$formattedDay"
  }

  /**
   * Creates a new date at the start of the month
   */
  fun atStartOfMonth(): LocalDate {
    return atDayOfMonth(DayOfMonth.FirstDay)
  }

  fun atStartOfNextMonth(): LocalDate {
    return atStartOfMonth().plusMonths(1)
  }

  /**
   * Creates a new instance with the new day of month
   */
  fun atDayOfMonth(newDayOfMonth: DayOfMonth): LocalDate {
    return LocalDate(year, month, newDayOfMonth)
  }

  fun withMonth(month: Month): LocalDate {
    val updatedDayOfMonth = dayOfMonth.coerceDayOfMonth(year, month)
    return LocalDate(year, month, updatedDayOfMonth)
  }

  fun withDay(dayOfMonth: DayOfMonth): LocalDate {
    return LocalDate(year, month, dayOfMonth)
  }

  /**
   * Adds months; the day of month is coerced to the length of the target month.
   */
  fun plusMonths(monthsToAdd: Long): LocalDate {
    if (monthsToAdd == 0L) {
      return this
    }
    require(monthsToAdd in -MaxMonthSpan..MaxMonthSpan) {
      "Months to add $monthsToAdd outside -$MaxMonthSpan..$MaxMonthSpan"
    }

    // Zero-based month index across the epoch, so floorDiv/mod also work backwards across year boundaries.
    val totalMonths = (month.value - 1) + monthsToAdd
    val newYear = year + totalMonths.floorDiv(12L)
    val newMonth = Month(totalMonths.mod(12L).toInt() + 1)

    return LocalDate(newYear, newMonth, dayOfMonth.coerceDayOfMonth(newYear, newMonth))
  }

  fun plusDays(daysToAdd: Long): LocalDate {
    val epochDay = toEpochDay()
    require(daysToAdd in (MinEpochDay - epochDay)..(MaxEpochDay - epochDay)) {
      "Days to add $daysToAdd to $this leave ${Year.Min}..${Year.Max}"
    }
    return ofEpochDay(epochDay + daysToAdd)
  }

  /**
   * Adds years; February 29 becomes February 28 in a common year.
   */
  fun plusYears(yearsToAdd: Long): LocalDate {
    require(yearsToAdd in -MaxYearSpan..MaxYearSpan) {
      "Years to add $yearsToAdd outside -$MaxYearSpan..$MaxYearSpan"
    }
    val newYear = year + yearsToAdd
    return LocalDate(newYear, month, dayOfMonth.coerceDayOfMonth(newYear, month))
  }

  override fun compareTo(other: LocalDate): Int {
    return when {
      this.year != other.year -> this.year.compareTo(other.year)
      this.month != other.month -> this.month.compareTo(other.month)
      else -> this.dayOfMonth.compareTo(other.dayOfMonth)
    }
  }

  override fun toString(): String {
    return format()
  }

  companion object {
    private const val DaysPerGregorianEra: Long = 146_097L
    private const val DaysPerJulianCycle: Long = 1_461L

    /**
     * Day of 0000-03-01 counted from 1970-01-01, negated.
     */
    private const val GregorianEpochShift: Long = 719_468L

    /**
     * Day of julian 0000-03-01 counted from gregorian 1970-01-01, negated.
     */
    private const val JulianEpochShift: Long = 719_470L

    private const val MaxYearSpan: Long = Year.MaxValue - Year.MinValue
    private const val MaxMonthSpan: Long = MaxYearSpan * 12

    /**
     * The epoch day of `Year.Min-01-01`.
     */
    val MinEpochDay: Long = LocalDate(Year.Min, Month.January, DayOfMonth.FirstDay).toEpochDay()

    /**
     * The epoch day of `Year.Max-12-31`.
     */
    val MaxEpochDay: Long = LocalDate(Year.Max, Month.December, DayOfMonth(31)).toEpochDay()

    /**
     * The first millisecond of [MinEpochDay].
     */
    val MinMillis: @ms Double = MinEpochDay * TimeConstants.MillisPerDay

    /**
     * The end of [MaxEpochDay], exclusive.
     */
    val MaxMillis: @ms Double = (MaxEpochDay + 1) * TimeConstants.MillisPerDay

    operator fun invoke(
      year: Int,
      month: Int,
      dayOfMonth: Int,
    ): LocalDate {
      return LocalDate(Year(year), Month(month), DayOfMonth(dayOfMonth))
    }

    /**
     * The date [epochDay] days after 1970-01-01, algorithm `civil_from_days` by Howard Hinnant.
     */
    fun ofEpochDay(epochDay: Long): LocalDate {
      require(epochDay in MinEpochDay..MaxEpochDay) {
        "Epoch day $epochDay outside $MinEpochDay..$MaxEpochDay"
      }
      val shiftedDay = epochDay + GregorianEpochShift
      val era = shiftedDay.floorDiv(DaysPerGregorianEra)
      val dayOfEra = shiftedDay - era * DaysPerGregorianEra
      val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
      val dayOfShiftedYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
      return fromShiftedYear(yearOfEra + era * 400, dayOfShiftedYear)
    }

    /**
     * The days of [month] in the julian calendar, whose leap year is every fourth year.
     */
    fun daysInJulianMonth(year: Year, month: Month): Int {
      return when {
        month != Month.February -> month.daysInMonth(year).value
        year.value % 4L == 0L -> 29
        else -> 28
      }
    }

    /**
     * The gregorian date of a date given in the julian calendar, whose leap year is every fourth year.
     *
     * Example: julian 1582-10-04 is gregorian 1582-10-14.
     */
    fun ofJulian(year: Year, month: Month, dayOfMonth: DayOfMonth): LocalDate {
      require(dayOfMonth.value in 1..daysInJulianMonth(year, month)) {
        "Invalid julian date: day ${dayOfMonth.value} outside 1..${daysInJulianMonth(year, month)} in $year-${month.value}"
      }
      val monthValue = month.value
      val shiftedYear = if (monthValue <= 2) year.value - 1 else year.value
      val cycle = shiftedYear.floorDiv(4L)
      val yearOfCycle = shiftedYear - cycle * 4
      val dayOfCycle = yearOfCycle * 365 + dayOfShiftedYear(monthValue, dayOfMonth.value)
      return ofEpochDay(cycle * DaysPerJulianCycle + dayOfCycle - JulianEpochShift)
    }

    /**
     * The date at [millis] shifted by the time zone [offset] (local minus UTC).
     */
    fun fromMillis(millis: @ms Double, offset: @ms Double): LocalDate {
      requireMillisInRange(millis + offset)
      return ofEpochDay(epochDayAt(millis + offset))
    }

    /**
     * Parses ISO 8601 with expanded year: `-0043-03-15`, `1789-07-14`, `+10000-01-01`.
     */
    fun parse(decodeString: String): LocalDate {
      val match = isoDatePattern.matchEntire(decodeString).requireNotNull {
        "Invalid date format: $decodeString"
      }
      val (sign, yearDigits, monthDigits, dayDigits) = match.destructured
      val absoluteYear = yearDigits.toLongOrNull().requireNotNull {
        "Year $sign$yearDigits outside ${Year.MinValue}..${Year.MaxValue}"
      }
      val year = if (sign == "-") -absoluteYear else absoluteYear
      return LocalDate(Year(year), Month(monthDigits.toInt()), DayOfMonth(dayDigits.toInt()))
    }

    private val isoDatePattern: Regex = Regex("""([+-]?)(\d+)-(\d{2})-(\d{2})""")

    /**
     * Day within a year that starts on March 1, so the leap day is the last day.
     */
    private fun dayOfShiftedYear(monthValue: Int, dayOfMonthValue: Int): Long {
      val monthOfShiftedYear = if (monthValue > 2) monthValue - 3 else monthValue + 9
      return ((153 * monthOfShiftedYear + 2) / 5 + dayOfMonthValue - 1).toLong()
    }

    private fun fromShiftedYear(shiftedYear: Long, dayOfShiftedYear: Long): LocalDate {
      val monthOfShiftedYear = ((5 * dayOfShiftedYear + 2) / 153).toInt()
      val dayOfMonthValue = (dayOfShiftedYear - (153 * monthOfShiftedYear + 2) / 5 + 1).toInt()
      val monthValue = if (monthOfShiftedYear < 10) monthOfShiftedYear + 3 else monthOfShiftedYear - 9
      val yearValue = if (monthValue <= 2) shiftedYear + 1 else shiftedYear
      return LocalDate(Year(yearValue), Month(monthValue), DayOfMonth(dayOfMonthValue))
    }

    /**
     * The epoch day of the local time [localMillis] (UTC plus offset): the last day whose start, as `Double`, is not after it.
     *
     * Beyond 292 million years from 1970 the start of a day has no exact `Double`; the division alone would read such a start as the day before.
     */
    internal fun epochDayAt(localMillis: @ms Double): Long {
      val epochDay = floor(localMillis / TimeConstants.MillisPerDay).toLong()
      return when {
        (epochDay + 1) * TimeConstants.MillisPerDay <= localMillis -> epochDay + 1
        epochDay * TimeConstants.MillisPerDay > localMillis -> epochDay - 1
        else -> epochDay
      }
    }

    /**
     * Requires the local time [localMillis] (UTC plus offset) within the days of `Year.Min` to `Year.Max`.
     */
    internal fun requireMillisInRange(localMillis: @ms Double) {
      require(localMillis >= MinMillis && localMillis < MaxMillis) {
        "Local millis $localMillis outside $MinMillis..$MaxMillis"
      }
    }
  }
}

/**
 * The instant at which this date starts in [timeZone], resolved like [LocalDateTime.toMillis].
 */
fun LocalDate.toMillisAtStartOfDay(timeZone: TimeZone): @ms Double {
  return LocalDateTime(this, LocalTime.Midnight).toMillis(timeZone)
}

/**
 * The date at [millis] in [expectedTimeZone], with the offset from [timeZoneOffsetProvider].
 */
fun LocalDate.Companion.fromMillisCurrentTimeZone(millis: @ms Double, expectedTimeZone: TimeZone): LocalDate {
  return fromMillis(millis, timeZoneOffsetProvider.offset(millis, expectedTimeZone))
}
