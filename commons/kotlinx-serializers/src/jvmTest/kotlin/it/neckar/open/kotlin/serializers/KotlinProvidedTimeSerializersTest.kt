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
package it.neckar.open.kotlin.serializers

import it.neckar.open.serialization.roundTrip
import it.neckar.open.time.TimeInstantExample
import org.junit.jupiter.api.Test

/**
 *
 */
class KotlinProvidedTimeSerializersTest {
  @Test
  fun testInstantRoundtrip() {
    roundTrip(TimeInstantExample._2026_04_24_03_10_00_000Z){
      """
        "2026-04-24T03:10:00Z"
      """.trimIndent()
    }

    roundTrip(TimeInstantExample._2024_09_12_06_44_17_500Z){
      """
        "2024-09-12T06:44:17.500Z"
      """.trimIndent()
    }

    roundTrip(TimeInstantExample._2019_01_07_15_45_19_123_987_345_678_901){
      """
        "2019-01-07T15:45:19.123987304Z"
      """.trimIndent()
    }
  }
}
