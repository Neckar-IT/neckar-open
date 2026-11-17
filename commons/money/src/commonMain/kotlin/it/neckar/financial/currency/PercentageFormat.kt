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

import it.neckar.open.formatting.formatAsPercentage
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.unit.other.pct
import kotlin.math.roundToLong

/**
 * This fraction as a percentage with as many decimals as it carries, at most two: `19 %`, `5,5 %`, `2,75 %`.
 */
fun @pct Double.formatAsPercentageUpToTwoDecimals(i18nConfiguration: I18nConfiguration = I18nConfiguration.Germany, whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
  val basisPoints = (this * VatRate.BasisPointsPerUnit).roundToLong()
  val numberOfDecimals: Int = when {
    basisPoints % BasisPointsPerPercent == 0L -> 0
    basisPoints % (BasisPointsPerPercent / 10) == 0L -> 1
    else -> 2
  }
  return formatAsPercentage(numberOfDecimals = numberOfDecimals, i18nConfiguration = i18nConfiguration, whitespaceConfig = whitespaceConfig)
}

private const val BasisPointsPerPercent: Long = 100
