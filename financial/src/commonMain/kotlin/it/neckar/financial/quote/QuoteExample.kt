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
package it.neckar.financial.quote

import it.neckar.commons.tags.Tags
import it.neckar.customer.CustomerExample
import it.neckar.customer.company.CompanyExample
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroNet
import it.neckar.financial.priced.PricedElements
import it.neckar.financial.priced.PricedItem
import it.neckar.financial.priced.PricedSection
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

object QuoteExample {
  val filledQuote: Quote = Quote(
    id = Quote.Id(Uuid.parse("5fa9e188-d2c7-44fa-a9e1-88d2c764faca")),
    sellingCompany = CompanyExample.ElektroTechnikMueller,
    belongsTo = Uuid.parse("41bede92-b905-4b92-bede-92b9058b92df"),
    title = "Beispielangebot",
    maintainerName = "Max Mustermann",
    customer = CustomerExample.EmmaWilliams,
    pricedElements = PricedElements(
      elements = listOf(
        PricedSection(
          uuid = Uuid.parse("d2c7e188-5fa9-44fa-a9e1-88d2c764faca"),
          headline = "Installation von Elektrogeräten",
          amount = Amount(amount = 1.0),
          factor = 1.0,
          visibility = Visibility.Public,
          details = "Installation von Elektrogeräten in der Wohnung",
          children = listOf(
            PricedItem(
              uuid = Uuid.parse("052e09a2-f99e-43db-ae09-a2f99e13db4a"),
              headline = "Installation der Küche",
              amount = Amount(amount = 1.0),
              factor = 1.0,
              visibility = Visibility.Public,
              details = "Installation der Elektrogeräte in der Küche",
              priceForOneElement = PriceWithProfit.noProfit(500.0.euroNet),
              relevance = Tags.empty,
              optionality = Optionality.Mandatory,
              vatRate = VatRate.NineteenPercent,
            ),
            PricedItem(
              uuid = Uuid.parse("0b4403e5-9809-4aa2-8403-e598096aa291"),
              headline = "Installation des Wohnzimmers",
              amount = Amount(
                amount = 200.0,
                unit = AmountUnit.None
              ),
              factor = 1.0,
              visibility = Visibility.Public,
              details = "Installation der Elektrogeräte im Wohnzimmer",
              priceForOneElement = PriceWithProfit.noProfit(300.0.euroNet),
              relevance = Tags.empty,
              optionality = Optionality.Mandatory,
              vatRate = VatRate.NineteenPercent,
            ),

          )
        ),
      ),
      discountPercentage = 0.0,
    ),
    sources = QuoteSources(
      softwareVersion = "1.0.0",
      basePriceListId = PriceListExample.Current.uuid
    ),
    creationDate = LocalDate(2024, 7, 15),
    validUntil = LocalDate(2024, 8, 14),
    quoteNumber = "2024-1234-1",
    deliveryTimeInMonths = 0,
    legalNotice = QuoteLegalNotice.empty,
  )

  val emptyQuote: Quote = Quote(
    id = Quote.Id(Uuid.parse("53814b30-e677-4cd3-814b-30e6776cd380")),
    sellingCompany = CompanyExample.ElektroTechnikMueller,
    belongsTo = Uuid.parse("41bede92-b905-4b92-bede-92b9058b92df"),
    title = "Beispielangebot",
    maintainerName = "Max Mustermann",
    customer = CustomerExample.EmmaWilliams,
    pricedElements = PricedElements(
      elements = emptyList(),
      discountPercentage = 0.0,
    ),
    sources = QuoteSources(
      softwareVersion = "1.0.0",
      /**
       * refers to a base price list that does not exist in the example data
       */
      basePriceListId = Uuid.parse("cba57dae-6207-4a80-a57d-ae62076a8059")
    ),
    creationDate = LocalDate(2025, 7, 9),
    validUntil = LocalDate(2025, 8, 8),
    quoteNumber = "2025-1234-1",
    deliveryTimeInMonths = 0,
    legalNotice = QuoteLegalNotice.empty,
  )


  val entries: List<Quote> = listOf(
    filledQuote
  )
}
