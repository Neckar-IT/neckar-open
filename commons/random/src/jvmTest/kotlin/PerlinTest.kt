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
import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.fastFor
import it.neckar.open.random.Perlin
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class PerlinTest {
  @Test
  fun testPerlin() {
    val perlin = Perlin(1234.0)
    val out = perlin.noise(15.0)

    assertThat(out).isEqualTo(0.5283421107700893)
  }

  @Test
  fun testPerlinSeed() {
    val perlin1 = Perlin(1234.0)
    val out = perlin1.noise(15.0)

    val perlin2 = Perlin(1233214.0)
    val outDiffSeed = perlin2.noise(15.0)

    val perlin3 = Perlin(1234.0)
    val outSameSeed = perlin3.noise(15.0)

    assertThat(out).isNotEqualTo(outDiffSeed)
    assertThat(out).isEqualTo(outSameSeed)
  }


  @Test
  fun testPerlinFreq() {
    val perlin = Perlin(1234.0)
    val out = perlin.noise(15.0)

    val perlinDiffFreq = Perlin(1234.0,2.0)
    val outDiffFreq = perlinDiffFreq.noise(15.0)
    val outAdjusted = perlinDiffFreq.noise(15 / 2.0) // divide by 2 to get to the same point as the same result on a different (2.0) frequency

    assertThat(out).isNotEqualTo(outDiffFreq)
    assertThat(out).isEqualTo(outAdjusted)
  }

  @Test
  fun testPerlinAmpl() {
    val perlin = Perlin(1234.0, 1.0, 1.0)
    val out = perlin.noise(15.0)

    val perlinDiffAmpl = Perlin(1234.0, 1.0, 2.0)
    val outDiffAmpl = perlinDiffAmpl.noise(15.0)

    assertThat(out).isNotEqualTo(outDiffAmpl);
    assertThat(out).isLessThan(outDiffAmpl);
  }

  @Disabled
  @Test
  fun testPerlinMinMax() {
    val perlin = Perlin(123123.0)

    var min = Double.MAX_VALUE
    var max = -Double.MAX_VALUE

    1_000_000.fastFor {
      val noise = perlin.noise(it.toDouble())
      min = min.coerceAtMost(noise)
      max = max.coerceAtLeast(noise)
    }

    assertThat(min).isCloseTo(0.054, 0.01)
    assertThat(max).isCloseTo(0.94, 0.01)
  }
}
