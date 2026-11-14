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
package it.neckar.financial.quote.input

import assertk.*
import assertk.assertions.*
import it.neckar.commons.tags.Tags
import it.neckar.contact.EmailAddress
import it.neckar.customer.Customer
import it.neckar.customer.company.Company
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.euroNet
import it.neckar.financial.priced.input.PricedElementsInput
import it.neckar.financial.priced.input.PricedItemInput
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Quote
import it.neckar.financial.quote.QuoteLegalNotice
import it.neckar.financial.quote.QuoteSources
import it.neckar.financial.quote.Visibility
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

/**
 * The envelope a pricing script writes - it states every value but the quote's id and the item uuids,
 * which it may leave to the conversion.
 */
class QuoteInputTest {
  @Test
  fun `testToQuote keeps what a producer states`() {
    val quote = quoteInput.toQuote()

    assertThat(quote.id).isEqualTo(Quote.Id(Uuid.parse("00000000-0000-0000-0000-0000000000a1")))
    assertThat(quote.creationDate).isEqualTo(LocalDate(2026, 4, 1))
    assertThat(quote.validUntil).isEqualTo(LocalDate(2026, 5, 1))
    assertThat(quote.deliveryTimeInMonths).isEqualTo(0)
    assertThat(quote.legalNotice.additionalLines).containsExactly("Freibleibendes Angebot")
    assertThat(quote.netPrice).isEqualTo(200.0.euroNet)
  }

  @Test
  fun `testToQuote generates an id where the producer states none`() {
    val withoutId = quoteInput.copy(id = null)

    assertThat(withoutId.toQuote().id).isNotEqualTo(withoutId.toQuote().id)
  }

  @Test
  fun `testDecode the document a producer writes`() {
    val decoded = Json.decodeFromString(
      QuoteInput.serializer(),
      //language=JSON
      """
        {
          "id" : "00000000-0000-0000-0000-0000000000a1",
          "sellingCompany" : {
            "id" : "00000000-0000-0000-0000-0000000000a2",
            "namespace" : "book-order-preview",
            "name" : "Dummy sellingCompany"
          },
          "belongsTo" : "00000000-0000-0000-0000-0000000000a5",
          "title" : "Bestellung",
          "maintainerName" : "",
          "customer" : {
            "id" : "00000000-0000-0000-0000-0000000000a3",
            "firstName" : "Ada",
            "lastName" : "Lovelace",
            "email" : "ada@example.com"
          },
          "pricedElements" : {
            "elements" : [ {
              "type" : "PricedItemInput",
              "uuid" : "00000000-0000-0000-0000-0000000000b1",
              "headline" : "Ein Buch",
              "details" : null,
              "priceForOneElement" : {
                "inputPrice" : 10000,
                "sellingPrice" : 10000
              },
              "amount" : {
                "amount" : 2.0
              },
              "visibility" : "Public",
              "relevance" : [ ],
              "vatRate" : 0.19
            } ],
            "discountPercentage" : 0.0
          },
          "sources" : {
            "softwareVersion" : "0.0.0-dummy",
            "basePriceListId" : "00000000-0000-0000-0000-0000000000a4"
          },
          "creationDate" : "2026-04-01",
          "validUntil" : "2026-05-01",
          "quoteNumber" : "DUMMY-0001",
          "deliveryTimeInMonths" : 0,
          "legalNotice" : {
            "additionalLines" : [ "Freibleibendes Angebot" ]
          }
        }
      """.trimIndent()
    )

    assertThat(decoded).isEqualTo(quoteInput)
  }

  private val quoteInput: QuoteInput = QuoteInput(
    id = Quote.Id(Uuid.parse("00000000-0000-0000-0000-0000000000a1")),
    sellingCompany = Company.empty(
      id = Company.Id(Uuid.parse("00000000-0000-0000-0000-0000000000a2")),
      namespace = "book-order-preview",
      name = "Dummy sellingCompany",
    ),
    belongsTo = Uuid.parse("00000000-0000-0000-0000-0000000000a5"),
    title = "Bestellung",
    maintainerName = "",
    customer = Customer(
      id = Customer.Id(Uuid.parse("00000000-0000-0000-0000-0000000000a3")),
      firstName = "Ada",
      lastName = "Lovelace",
      email = EmailAddress("ada@example.com"),
    ),
    pricedElements = PricedElementsInput(
      elements = listOf(
        PricedItemInput(
          uuid = Uuid.parse("00000000-0000-0000-0000-0000000000b1"),
          headline = "Ein Buch",
          details = null,
          priceForOneElement = PriceWithProfit.noProfit(100.0.euroNet),
          amount = Amount(2.0),
          visibility = Visibility.Public,
          relevance = Tags.empty,
          vatRate = VatRate.NineteenPercent,
        ),
      ),
      discountPercentage = 0.0,
    ),
    sources = QuoteSources(
      softwareVersion = "0.0.0-dummy",
      basePriceListId = Uuid.parse("00000000-0000-0000-0000-0000000000a4"),
    ),
    creationDate = LocalDate(2026, 4, 1),
    validUntil = LocalDate(2026, 5, 1),
    quoteNumber = "DUMMY-0001",
    deliveryTimeInMonths = 0,
    legalNotice = QuoteLegalNotice(listOf("Freibleibendes Angebot")),
  )
}
