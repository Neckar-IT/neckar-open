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
package it.neckar.financial.priced

import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroNet
import it.neckar.financial.quote.Visibility
import it.neckar.open.kotlin.lang.percent

object PricedElementsExample {

  val simple: PricedElements = PricedElements {
    section("Elektroinstallation") {
      visibility = Visibility.Public
      item(
        headline = "Steckdosen",
        priceForOneElement = PriceWithProfit.surcharge(10.00.euroNet, 25.0.percent),
        amount = 10.0,
        relevance = Tags.empty,
        visibility = Visibility.Public,
        vatRate = VatRate.NineteenPercent,
      )
      item(
        headline = "Lichtschalter",
        priceForOneElement = PriceWithProfit.surcharge(7.00.euroNet, 27.0.percent),
        amount = 5.0,
        relevance = Tags.empty,
        visibility = Visibility.Public,
        vatRate = VatRate.NineteenPercent,
      )
    }
  }

  val withDiscount: PricedElements = PricedElements {
    discountPercentage = 5.0.percent
    section("PV-Anlage") {
      visibility = Visibility.Public
      item(
        headline = "Solarmodule",
        priceForOneElement = PriceWithProfit.surcharge(280.00.euroNet, 25.0.percent),
        amount = 20.0,
        relevance = Tags.empty,
        visibility = Visibility.Public,
        vatRate = VatRate.Zero,
      )
    }
  }

  val mixedVatRates: PricedElements = PricedElements {
    section("Elektroinstallation") {
      visibility = Visibility.Public
      netItem("Verteilerschrank", 1_234.57.euroNet, VatRate.NineteenPercent)
      netItem("Leitungen", 89.13.euroNet, VatRate.NineteenPercent)
      netItem("Reparatur Bestandsanlage", 456.79.euroNet, VatRate.SevenPercent)
      netItem("Wartungspauschale", 77.77.euroNet, VatRate.SevenPercent)
    }
  }

  /**
   * A quote with credit items for inclusive installations that were not placed — they reduce the taxable base.
   */
  val withCreditItems: PricedElements = PricedElements {
    section("Elektroinstallation") {
      visibility = Visibility.Public
      netItem("Steckdosen", 302.55.euroNet, VatRate.NineteenPercent)
      netItem("Gutschrift nicht platzierte Inklusiv-Installation", (-74.50).euroNet, VatRate.NineteenPercent)
      netItem("Gutschrift nicht platzierte Inklusiv-Installation", (-32.25).euroNet, VatRate.NineteenPercent)
    }
  }

  /**
   * Credit items combined with a discount — the only place a negative amount is rounded, since the discount is applied per item.
   * [it.neckar.financial.currency.Money.Net.timesAndRound] rounds ties towards positive infinity, which is asymmetric for negative amounts:
   * `-74.50 × 0.93 = -69.285` becomes `-69.28`, not `-69.29`.
   */
  val withCreditItemsAndDiscount: PricedElements = PricedElements {
    discountPercentage = 7.0.percent
    section("Elektroinstallation") {
      visibility = Visibility.Public
      netItem("Steckdosen", 302.55.euroNet, VatRate.NineteenPercent)
      netItem("Gutschrift nicht platzierte Inklusiv-Installation", (-74.50).euroNet, VatRate.NineteenPercent)
      netItem("Gutschrift nicht platzierte Inklusiv-Installation", (-32.25).euroNet, VatRate.NineteenPercent)
    }
  }

  /**
   * A quote with a discount and taxed items — the discount is applied per item.
   */
  val withDiscountAndVat: PricedElements = PricedElements {
    discountPercentage = 7.0.percent
    section("Elektroinstallation") {
      visibility = Visibility.Public
      netItem("Verteilerschrank", 1_234.57.euroNet, VatRate.NineteenPercent)
      netItem("Leitungen", 89.13.euroNet, VatRate.NineteenPercent)
      netItem("Reparatur Bestandsanlage", 456.79.euroNet, VatRate.SevenPercent)
    }
  }

  /**
   * The smallest quote in which the two possible rounding bases disagree: the aggregated net of `84.06` is taxed
   * `round(84.06 × 0.19) = 15.97`, while rounding per item would give `round(42.03 × 0.19) × 2 = 15.98`.
   */
  val twoItemsWithRoundingDivergence: PricedElements = PricedElements {
    section("Elektroinstallation") {
      visibility = Visibility.Public
      netItem("Steckdose", 42.03.euroNet, VatRate.NineteenPercent)
      netItem("Steckdose", 42.03.euroNet, VatRate.NineteenPercent)
    }
  }

  /**
   * The 15 item quote from the manual pre-deployment test that surfaced the inconsistent sum block.
   * Net `1,694.17 €`, tax `321.89 €`, gross `2,016.06 €` — summing the per-item gross prices would give `2,016.07 €` instead.
   */
  val fifteenItems: PricedElements = PricedElements {
    section("Wohnzimmer") {
      visibility = Visibility.Public
      netItem("Schuko-Steckdose", 49.95.euroNet, VatRate.NineteenPercent)
      netItem("Schuko-Steckdose", 49.95.euroNet, VatRate.NineteenPercent)
      netItem("Schuko-Steckdose", 49.95.euroNet, VatRate.NineteenPercent)
      netItem("Zusatzinstallation Außenbereich", 189.00.euroNet, VatRate.NineteenPercent)
      netItem("Zusatzposition Kabelkanal", 68.50.euroNet, VatRate.NineteenPercent, amount = 2.0)
      netItem("Gutschrift nicht platzierte Inklusiv-Installation", (-74.50).euroNet, VatRate.NineteenPercent)
      netItem("Gutschrift nicht platzierte Inklusiv-Installation", (-32.25).euroNet, VatRate.NineteenPercent)
    }
    section("Ausstattung") {
      visibility = Visibility.Public
      netItem("Rauchwarnmelder", 202.90.euroNet, VatRate.NineteenPercent)
      netItem("Bewegungsmelder", 139.50.euroNet, VatRate.NineteenPercent)
      netItem("Dimmer", 157.50.euroNet, VatRate.NineteenPercent)
      netItem("Unterverteilung", 233.00.euroNet, VatRate.NineteenPercent)
      netItem("Netzwerkdosen", 159.90.euroNet, VatRate.NineteenPercent)
      netItem("Klingelanlage", 140.95.euroNet, VatRate.NineteenPercent)
      netItem("Außenleuchten", 149.13.euroNet, VatRate.NineteenPercent)
      netItem("Kabeltrassen", 142.19.euroNet, VatRate.NineteenPercent)
    }
  }

  val onlyZeroVat: PricedElements = PricedElements {
    section("PV-Anlage") {
      visibility = Visibility.Public
      netItem("Solarmodule", 5_600.00.euroNet, VatRate.Zero)
      netItem("Wechselrichter", 1_234.56.euroNet, VatRate.Zero)
    }
  }

  val singleItem: PricedElements = PricedElements {
    section("Elektroinstallation") {
      visibility = Visibility.Public
      netItem("Verteilerschrank", 1_234.57.euroNet, VatRate.NineteenPercent)
    }
  }

  val empty: PricedElements = PricedElements.empty

  val entries: Map<String, PricedElements> = mapOf(
    "simple" to simple,
    "withDiscount" to withDiscount,
    "mixedVatRates" to mixedVatRates,
    "withCreditItems" to withCreditItems,
    "withCreditItemsAndDiscount" to withCreditItemsAndDiscount,
    "withDiscountAndVat" to withDiscountAndVat,
    "twoItemsWithRoundingDivergence" to twoItemsWithRoundingDivergence,
    "fifteenItems" to fifteenItems,
    "onlyZeroVat" to onlyZeroVat,
    "singleItem" to singleItem,
    "empty" to empty,
  )
}

/**
 * Adds a public item whose net price is exactly [netPricePerElement] multiplied by [amount] — no profit surcharge, no factor.
 */
@IgnorableReturnValue
private fun PricedSectionBuilder.netItem(headline: String, netPricePerElement: Money.Net, vatRate: VatRate, amount: Double = 1.0): PricedItemBuilder {
  return item(
    headline = headline,
    priceForOneElement = PriceWithProfit.noProfit(netPricePerElement),
    amount = amount,
    relevance = Tags.empty,
    visibility = Visibility.Public,
    vatRate = vatRate,
  )
}
