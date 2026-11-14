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
package it.neckar.open.formatting

import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig

/**
 * The formatted date is always UTC.
 */
actual class DateTimeFormatIso8601 : DateTimeFormat {
  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return jsToIsoString(timestamp)
  }

  actual companion object {
    /**
     * Parses the date-time string to a timestamp
     */
    actual fun parse(formattedIsoString: String): Double {
      return jsDateParse(formattedIsoString)
    }
  }
}

actual class DateFormatIso8601 : DateTimeFormat {
  //The en-CA locale formats as YYYY-MM-DD - the locale parameter is fixed, only the time zone varies
  private val formatter = CachedIntlDateTimeFormat(::jsCreateIsoDateFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return formatter.format(timestamp, "en-CA", i18nConfiguration.timeZone.zoneId)
  }
}

actual class TimeFormatIso8601 : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateIsoTimeFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId) +
      "." + jsMilliseconds(timestamp).formatWithLeadingZeros(3)
  }
}

/**
 * The formatted date is always UTC.
 */
actual class DateTimeFormatUTC : DateTimeFormat {
  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return jsToIsoString(timestamp)
  }

  actual companion object {
    /**
     * Parses the UTC string to a timestamp
     */
    actual fun parse(formattedUtc: String): Double {
      return jsDateParse(formattedUtc)
    }
  }
}

/**
 * A formatted date (no date)
 */
actual class TimeFormat : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateTimeFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId)
  }
}

actual class TimeFormatWithMillis : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateTimeFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    val formattedTimeWithoutMillis = formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId)
    return insertMillis(formattedTimeWithoutMillis, jsUtcMilliseconds(timestamp))
  }
}

actual class DateFormat : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateDateFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId)
  }
}

actual class DefaultDateTimeFormat : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateDateTimeFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId)
  }
}

actual class DateTimeFormatShort : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateDateTimeShortFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId)
  }
}

actual class DateTimeFormatWithMillis : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateDateTimeFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    val formattedDateTimeWithoutMillis = formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId)
    return insertMillis(formattedDateTimeWithoutMillis, jsUtcMilliseconds(timestamp))
  }
}

actual class DateTimeFormatShortWithMillis : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateDateTimeShortFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    val formattedDateTimeWithoutMillis = formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId)
    return insertMillis(formattedDateTimeWithoutMillis, jsUtcMilliseconds(timestamp))
  }
}

/**
 * A format that formats a date - but only prints the month and year
 */
actual class YearMonthFormat actual constructor() : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateYearMonthFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId)
  }
}

actual class YearFormat actual constructor() : DateTimeFormat {
  private val formatter = CachedIntlDateTimeFormat(::jsCreateYearFormat)

  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return formatter.format(timestamp, i18nConfiguration.formatLocale.locale, i18nConfiguration.timeZone.zoneId)
  }
}

/**
 * Formats a time stamp as second with millis
 */
actual class SecondMillisFormat actual constructor() : DateTimeFormat {
  actual override fun format(timestamp: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    val number = jsSeconds(timestamp) + jsMilliseconds(timestamp) / 1000.0
    return number.format(
      numberOfDecimals = 3,
      i18nConfiguration = i18nConfiguration
    )
  }
}

/**
 * Caches the Intl.DateTimeFormat instance per (locale, timeZone) - creating a formatter on every
 * format() call is far more expensive than calling format() on a cached one.
 * Same pattern as DecimalFormat.formattersByLocale in NumberFormat.wasmJs.kt.
 * The format classes are cached themselves (dateTimeFormat etc. in commonMain), so these maps stay tiny.
 */
private class CachedIntlDateTimeFormat(
  private val createFormatter: (locale: String, timeZone: String) -> JsAny,
) {
  private val formatters = HashMap<String, JsAny>()

  fun format(millis: Double, locale: String, timeZone: String): String {
    val formatter = formatters.getOrPut("$locale|$timeZone") { createFormatter(locale, timeZone) }
    return jsIntlFormat(formatter, millis)
  }
}

private fun jsIntlFormat(formatter: JsAny, millis: Double): String = js("formatter.format(millis)")

//The explicit numeric options below are the spec-defined defaults of Date.toLocaleString /
//toLocaleDateString / toLocaleTimeString (ECMA-402 ToDateTimeOptions) - the output is identical
//to the previous per-call toLocale*String implementation.

private fun jsCreateDateTimeFormat(locale: String, timeZone: String): JsAny =
  js("new Intl.DateTimeFormat(locale, {timeZone: timeZone, year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric'})")

private fun jsCreateDateTimeShortFormat(locale: String, timeZone: String): JsAny =
  js("new Intl.DateTimeFormat(locale, {timeZone: timeZone, dateStyle: 'short', timeStyle: 'short'})")

private fun jsCreateDateFormat(locale: String, timeZone: String): JsAny =
  js("new Intl.DateTimeFormat(locale, {timeZone: timeZone, year: 'numeric', month: 'numeric', day: 'numeric'})")

private fun jsCreateTimeFormat(locale: String, timeZone: String): JsAny =
  js("new Intl.DateTimeFormat(locale, {timeZone: timeZone, hour: 'numeric', minute: 'numeric', second: 'numeric'})")

private fun jsCreateYearMonthFormat(locale: String, timeZone: String): JsAny =
  js("new Intl.DateTimeFormat(locale, {timeZone: timeZone, month: 'long', year: 'numeric'})")

private fun jsCreateYearFormat(locale: String, timeZone: String): JsAny =
  js("new Intl.DateTimeFormat(locale, {timeZone: timeZone, year: 'numeric'})")

private fun jsCreateIsoDateFormat(locale: String, timeZone: String): JsAny =
  js("new Intl.DateTimeFormat('en-CA', {timeZone: timeZone, year: 'numeric', month: 'numeric', day: 'numeric'})")

private fun jsCreateIsoTimeFormat(locale: String, timeZone: String): JsAny =
  js("new Intl.DateTimeFormat(locale, {timeZone: timeZone, hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit'})")

private fun jsToIsoString(millis: Double): String = js("new Date(millis).toISOString()")

private fun jsDateParse(formatted: String): Double = js("Date.parse(formatted)")

private fun jsSeconds(millis: Double): Int = js("new Date(millis).getSeconds()")

private fun jsMilliseconds(millis: Double): Int = js("new Date(millis).getMilliseconds()")

private fun jsUtcMilliseconds(millis: Double): Int = js("new Date(millis).getUTCMilliseconds()")
