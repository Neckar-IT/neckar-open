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
package it.neckar.financial.currency

import it.neckar.open.annotations.GermanTerm
import it.neckar.open.annotations.TsExport
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.unit.other.pct
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A fraction in whole basis points: 0.19 is 19 %.
 */
@Serializable
@JvmInline
@TsExport
@GermanTerm("Mehrwertsteuersatz")
value class VatRate(val vat: @pct Double) {
  init {
    require(vat.isFinite()) { "VAT rate must be finite: $vat" }
    require(vat in 0.0..1.0) { "VAT rate must lie between 0 and 1: $vat" }
    // A rate finer than a basis point is refused rather than silently rounded, so a tax calculation stays in integers.
    require(abs(vat * BasisPointsPerUnit - basisPoints) < BasisPointTolerance) { "VAT rate $vat is not a whole number of basis points" }
  }

  /**
   * The rate in hundredths of a percent.
   */
  val basisPoints: Int
    get() = (vat * BasisPointsPerUnit).roundToInt()

  fun format(i18nConfiguration: I18nConfiguration = I18nConfiguration.Germany, whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
    return "MwSt. ${formatAsPercentage(i18nConfiguration, whitespaceConfig)}"
  }

  /**
   * The rate without the "MwSt." prefix of [format]: `19 %`, `5,5 %`.
   */
  fun formatAsPercentage(i18nConfiguration: I18nConfiguration = I18nConfiguration.Germany, whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
    return vat.formatAsPercentageUpToTwoDecimals(i18nConfiguration, whitespaceConfig)
  }

  companion object {
    val Zero: VatRate = VatRate(0.0.percent)
    val SevenPercent: VatRate = VatRate(7.0.percent)
    val NineteenPercent: VatRate = VatRate(19.0.percent)

    const val BasisPointsPerUnit: Int = 10_000

    /** `vat * BasisPointsPerUnit` of a whole-basis-point rate misses the integer by the binary error of rate and product, far below this. */
    private const val BasisPointTolerance: Double = 1e-9
  }
}
