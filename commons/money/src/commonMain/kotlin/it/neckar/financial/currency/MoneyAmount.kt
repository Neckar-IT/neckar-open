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

import it.neckar.open.formatting.formatEuro
import it.neckar.open.i18n.CurrentI18nConfiguration
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.currency.ct

/**
 * An amount of euros whose tax basis is its type (ADL 0213): [Money] carries none, [Money.Net], [Money.Gross] and
 * [Money.Vat] carry theirs, and `Net * VatRate` and `Net + Vat` are the only operations that lead from one type to
 * another. Each of them is stored and serialized as its [cents] `Long`, so sums stay exact; sub-cent precision is
 * [MoneyFraction]. A field or parameter carries one of the four types; the interface is their shared formatting.
 */
sealed interface MoneyAmount {
  val cents: @ct Long

  /**
   * The euro value for display; it rounds, so no calculation starts from it.
   */
  val euros: @EUR Double
    get() = cents / 100.0

  fun format(i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration, whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
    return euros.formatEuro(i18nConfiguration, whitespaceConfig)
  }

  fun formatAsCents(whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
    return "${cents}${whitespaceConfig.smallSpace}ct"
  }

  fun formatOrIfZero(fallbackIfZero: String, i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration, whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
    if (isZero()) {
      return fallbackIfZero
    }
    return format(i18nConfiguration, whitespaceConfig)
  }

  fun isZero(): Boolean {
    return cents == 0L
  }
}
