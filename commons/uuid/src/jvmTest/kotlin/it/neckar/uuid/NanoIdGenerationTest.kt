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
package it.neckar.uuid

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.random
import it.neckar.open.test.utils.RandomWithSeed
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class NanoIdGenerationTest {
  @RandomWithSeed(75)
  @Test
  fun testRandomNanoId() {
    val id = NanoId.randomNanoId(random = random)
    assertThat(id.value).isEqualTo("xWJBvGD_7GyzKpRglSLMx")

    assertThat(id.value).hasLength(21)
    assertThat(NanoId.DefaultAlphabet).hasSize(64) //6 bits of information
    assertThat(21 * 6).isEqualTo(126)
  }

  @Test
  fun testCompareToUuid() {
    assertThat(Uuid.pseudoRandom().toByteArray()).hasSize(16)
    assertThat(16 * 8).isEqualTo(128) //UUID has 128 Bits - but 6 bits are hard coded
  }
}
