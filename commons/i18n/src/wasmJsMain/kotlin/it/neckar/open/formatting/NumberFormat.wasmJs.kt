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
import kotlin.math.pow

/*
 * See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/toLocaleString]
 */
actual class DecimalFormat internal actual constructor(
  /** Possible values are from 0 to 20 */
  actual override val maximumFractionDigits: Int,
  /** Possible values are from 0 to 20 */
  actual override val minimumFractionDigits: Int,
  /** Possible values are from 1 to 21 */
  actual override val minimumIntegerDigits: Int,
  /** Whether thousand separators should be used */
  actual override val useGrouping: Boolean,
) : NumberFormat, DecimalFormatDescriptor {

  /**
   * Caches the Intl.NumberFormat instance per locale - creating a formatter on every
   * format() call is far more expensive than calling format() on a cached one.
   * DecimalFormat instances themselves are cached (DecimalFormatsCache), so this map stays tiny.
   */
  private val formattersByLocale = HashMap<String, JsAny>()

  actual override fun format(value: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    val locale = i18nConfiguration.formatLocale.locale
    val formatter = formattersByLocale.getOrPut(locale) {
      jsCreateNumberFormat(locale, minimumIntegerDigits, minimumFractionDigits, maximumFractionDigits, useGrouping)
    }
    //add 0.0 to avoid "-0.0"
    return jsFormatNumber(formatter, value + 0.0)
  }

  override val precision: Double = 10.0.pow(-maximumFractionDigits)
}

/*
* See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/toExponential]
*/
actual class ExponentialFormat actual constructor(
  /** Possible values are from 0 to 20 */
  val maximumFractionDigits: Int,
  /** Possible values are from 0 to 20 */
  val minimumFractionDigits: Int,
  /** Possible values are from 1 to 21 */
  val minimumIntegerDigits: Int,
  /** Whether thousand separators should be used */
  val useGrouping: Boolean,
) : NumberFormat {

  actual override fun format(value: Double, i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return jsNumberToExponential(value, maximumFractionDigits)
  }

  override val precision: Double = 10.0.pow(-this.maximumFractionDigits)
}

private fun jsCreateNumberFormat(locale: String, minimumIntegerDigits: Int, minimumFractionDigits: Int, maximumFractionDigits: Int, useGrouping: Boolean): JsAny =
  js("new Intl.NumberFormat(locale, {minimumIntegerDigits: minimumIntegerDigits, minimumFractionDigits: minimumFractionDigits, maximumFractionDigits: maximumFractionDigits, useGrouping: useGrouping})")

private fun jsFormatNumber(formatter: JsAny, value: Double): String =
  js("formatter.format(value)")

private fun jsNumberToExponential(value: Double, maximumFractionDigits: Int): String =
  js("value.toExponential(maximumFractionDigits)")
