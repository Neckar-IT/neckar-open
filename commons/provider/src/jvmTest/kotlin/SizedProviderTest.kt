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
package it.neckar.open.provider

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test

class SizedProviderTest {
  @Test
  fun testMapped() {
    val list = listOf(0, 1, 2, 3)

    val sizedProvider = SizedProvider.mapped(list) {
      it.toString()
    }

    assertThat(sizedProvider.size()).isEqualTo(4)
    assertThat(sizedProvider.valueAt(0)).isEqualTo("0")
    assertThat(sizedProvider.valueAt(2)).isEqualTo("2")
  }

  @Test
  fun testSizedProvider1Foreach() {
    val provider = object : SizedProvider1<String, Number> {
      override fun size(param1: Number): Int {
        return 17
      }

      override fun valueAt(index: Int, param1: Number): String {
        return "$index - $param1"
      }
    }

    assertThat(provider.valueAt(12, 12.0)).isEqualTo("12 - 12.0")

    provider.fastForEachIndexed(12.0) { index, value ->
      assertThat(value).isEqualTo("$index - 12.0")
    }
  }
}
