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
package com.meistercharts.algorithms.layers.axis.time

import com.meistercharts.axis.time.DistanceYears
import com.meistercharts.axis.time.YearNumbering
import it.neckar.datetime.minimal.LocalDate
import it.neckar.datetime.minimal.LocalTime
import it.neckar.datetime.minimal.Year
import it.neckar.open.i18n.I18nConfiguration
import kotlin.math.abs

/**
 * The texts of a calendar label in German or English, selected by the text locale of [I18nConfiguration].
 */
enum class CalendarLabels(
  private val monthNames: List<String>,
  private val thousandsSeparator: Char,
  private val decimalSeparator: Char,
  private val million: String,
  private val billion: String,
  private val beforeChrist: String,
  private val today: String,
) {
  German(
    monthNames = listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"),
    thousandsSeparator = '.',
    decimalSeparator = ',',
    million = "Mio.",
    billion = "Mrd.",
    beforeChrist = "v. Chr.",
    today = "heute",
  ),
  English(
    monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
    thousandsSeparator = ',',
    decimalSeparator = '.',
    million = "million",
    billion = "billion",
    beforeChrist = "BC",
    today = "today",
  ),
  ;

  /**
   * A tick of [distance]: years before present or the historical year, as [YearNumbering.Calendar] places them.
   */
  fun year(year: Year, distance: DistanceYears): String {
    return if (distance.yearNumbering == YearNumbering.Calendar && YearNumbering.Calendar.countsBeforePresent(distance.distanceInYears)) {
      // The first tick may sit at Year.Min instead of a multiple of the distance; 100 million years keep "12,5 Mrd." exact.
      yearsBeforePresent(year, distance.distanceInYears.coerceAtMost(100_000_000L))
    } else {
      historicalYear(year)
    }
  }

  /**
   * "vor 66 Mio. Jahren", "66 million years ago", rounded to [resolutionYears]; counted from [YearNumbering.Calendar.BeforePresentReferenceYear].
   */
  fun yearsBeforePresent(year: Year, resolutionYears: Long): String {
    val yearsBeforePresent = YearNumbering.Calendar.BeforePresentReferenceYear - year.value
    val rounded = (yearsBeforePresent + resolutionYears / 2).floorDiv(resolutionYears) * resolutionYears
    if (rounded == 0L) {
      return today
    }
    val amount = amount(abs(rounded))
    return when (this) {
      German -> if (rounded > 0) "vor $amount Jahren" else "in $amount Jahren"
      English -> if (rounded > 0) "$amount years ago" else "in $amount years"
    }
  }

  /**
   * "1789", "44 v. Chr." for `Year(-43)`, "8.000 v. Chr.": historical counting without year 0.
   */
  fun historicalYear(year: Year): String {
    return if (year.value <= 0) "${grouped(1 - year.value)} $beforeChrist" else yearNumber(year.value)
  }

  /**
   * "Juli 1789", "July 1789"
   */
  fun month(date: LocalDate): String {
    return "${monthName(date)} ${historicalYear(date.year)}"
  }

  /**
   * "14. Juli", "July 14"
   */
  fun dayOfMonth(date: LocalDate): String {
    return when (this) {
      German -> "${date.dayOfMonth.value}. ${monthName(date)}"
      English -> "${monthName(date)} ${date.dayOfMonth.value}"
    }
  }

  /**
   * "14. Juli 1789", "July 14, 1789"
   */
  fun date(date: LocalDate): String {
    return when (this) {
      German -> "${dayOfMonth(date)} ${historicalYear(date.year)}"
      English -> "${dayOfMonth(date)}, ${historicalYear(date.year)}"
    }
  }

  /**
   * "14:00"
   */
  fun hoursMinutes(time: LocalTime): String {
    return "${twoDigits(time.hour)}:${twoDigits(time.minute)}"
  }

  /**
   * "14:00:05"
   */
  fun hoursMinutesSeconds(time: LocalTime): String {
    return "${hoursMinutes(time)}:${twoDigits(time.second)}"
  }

  /**
   * "14:00:00.100"
   */
  fun hoursMinutesSecondsMillis(time: LocalTime): String {
    return "${hoursMinutesSeconds(time)}.${time.millis.toString().padStart(3, '0')}"
  }

  private fun monthName(date: LocalDate): String {
    return monthNames[date.month.value - 1]
  }

  /**
   * "13,8 Mrd.", "66 Mio.", "12.000"
   */
  private fun amount(years: Long): String {
    return when {
      years >= 1_000_000_000L -> "${decimal(years, 1_000_000_000L)} $billion"
      years >= 1_000_000L -> "${decimal(years, 1_000_000L)} $million"
      else -> grouped(years)
    }
  }

  /**
   * [value] divided by [unit] with the decimals [value] has, at most three.
   */
  private fun decimal(value: Long, unit: Long): String {
    val thousandths = value.floorDiv(unit / 1000)
    val integerPart = grouped(thousandths / 1000)
    val fraction = (thousandths % 1000).toString().padStart(3, '0').trimEnd('0')
    return if (fraction.isEmpty()) integerPart else "$integerPart$decimalSeparator$fraction"
  }

  /**
   * Groups a year AD of five digits and more: "1789", "12.000".
   */
  private fun yearNumber(year: Long): String {
    return if (year >= 10_000L) grouped(year) else year.toString()
  }

  private fun grouped(value: Long): String {
    return value.toString().reversed().chunked(3).joinToString(thousandsSeparator.toString()).reversed()
  }

  private fun twoDigits(value: Int): String {
    return value.toString().padStart(2, '0')
  }

  companion object {
    fun of(i18nConfiguration: I18nConfiguration): CalendarLabels {
      return if (i18nConfiguration.textLocale.locale.startsWith("de")) German else English
    }
  }
}
