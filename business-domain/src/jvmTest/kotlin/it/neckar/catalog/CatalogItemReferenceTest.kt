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
package it.neckar.catalog

import assertk.assertFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import it.neckar.price.PriceTimeline
import it.neckar.domain.DomainIdentifier
import it.neckar.domain.HasDomainIdentifier
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.euroNet
import it.neckar.open.serialization.roundTrip
import it.neckar.open.time.TimeInstantExample
import it.neckar.uuid.parseUuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 *
 */
class CatalogItemReferenceTest {
  @Test
  fun testWrapping() {
    val id = CatalogItem.Id(parseUuid("d65a9138-1978-43a8-968b-2c4fc4cfdea5"))
    val pricedCatalogItem = MyDomainACatalogItem(
      id = id,
      title = "My Title",
      priceTimeline = PriceTimeline.single(17.49.euroNet),
    )

    roundTrip(pricedCatalogItem, DomainACatalogItem.serializer()) {
      //language=JSON
      """
        {
          "type" : "MyDomainACatalogItem",
          "id" : "d65a9138-1978-43a8-968b-2c4fc4cfdea5",
          "title" : "My Title",
          "description" : null,
          "icon" : null,
          "priceTimeline" : {
            "entries" : [ {
              "value" : 1749,
              "validFrom" : "2000-01-01T00:00:00Z"
            } ]
          },
          "domainIdentifier" : "domain-a",
          "typeKey" : "MyDomainACatalogItem"
        }
      """.trimIndent()
    }

    val ref = pricedCatalogItem.toReferenceAt(TimeInstantExample._2026_04_24_03_10_00_000Z)

    roundTrip(ref, CatalogItemReference.serializer()) {
      //language=JSON
      """
        {
          "referencedId" : "d65a9138-1978-43a8-968b-2c4fc4cfdea5",
          "referencedAt" : "2026-04-24T03:10:00Z",
          "domain" : "domain-a",
          "typeKey" : "MyDomainACatalogItem",
          "title" : "My Title",
          "price" : 1749,
          "icon" : null
        }
      """.trimIndent()
    }

  }

  @Test
  fun `blank title throws`() {
    assertFailure {
      CatalogItemReference(
        referencedId = CatalogItem.Id(parseUuid("d65a9138-1978-43a8-968b-2c4fc4cfdea5")),
        referencedAt = TimeInstantExample._2026_04_24_03_10_00_000Z,
        domain = DomainIdentifier("domain-a"),
        typeKey = CatalogItemTypeKey("MyDomainACatalogItem"),
        title = "  ",
        price = Money.Net.Zero,
      )
    }.isInstanceOf(IllegalArgumentException::class).messageContains("title must not be blank")
  }

  @Serializable
  private sealed interface DomainACatalogItem : PricedCatalogItem {
  }

  @Serializable
  @SerialName("MyDomainACatalogItem")
  private data class MyDomainACatalogItem(
    override val id: CatalogItem.Id,
    override val title: String,

    override val description: String? = null,
    override val icon: it.neckar.open.http.Url? = null,
    override val priceTimeline: PriceTimeline
  ) : DomainACatalogItem {
    override val domainIdentifier: it.neckar.domain.DomainIdentifier = MyDomainIdentifier.DOMAIN_A.domainIdentifier
    override val typeKey: CatalogItemTypeKey = CatalogItemTypeKey("MyDomainACatalogItem")

  }

  @Serializable
  private enum class MyDomainIdentifier(
    override val domainIdentifier: it.neckar.domain.DomainIdentifier
  ) : HasDomainIdentifier {
    DOMAIN_A(it.neckar.domain.DomainIdentifier("domain-a")),
    DOMAIN_B(DomainIdentifier("domain-b")),
  }
}
