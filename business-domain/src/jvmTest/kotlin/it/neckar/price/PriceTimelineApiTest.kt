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
package it.neckar.price

import assertk.*
import assertk.assertions.*
import it.neckar.timeline.TimelineEntry
import it.neckar.financial.currency.euroNet
import it.neckar.open.time.TimeInstantExample
import org.junit.jupiter.api.Test

/**
 *
 */
class PriceTimelineApiTest {

  @Test
  fun testHowToUse() {
    val priceTimeline = PriceTimeline(
      entries = listOf(
        TimelineEntry(
          value = 199.99.euroNet,
          validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z,
        ),
        TimelineEntry(
          value = 149.99.euroNet,
          validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
        ),
      )
    )

    val myFoo = MyFoo(
      name = "My foo",
      priceTimeline = PriceTimeline.empty()
    )

    assertThat(myFoo.newestPriceOrNull()).isNull()

    val foo = myFoo.copy(
      priceTimeline = priceTimeline.plus(
        TimelineEntry(
          value = 44.99.euroNet,
          validFrom = TimeInstantExample._2022_07_29_09_38_04_141Z,
        )
      )
    )

    assertThat(foo.newestPrice()).isEqualTo(199.99.euroNet)
  }

  data class MyFoo(
    val name: String,
    override val priceTimeline: PriceTimeline
  ) : HasPriceTimeline
}
