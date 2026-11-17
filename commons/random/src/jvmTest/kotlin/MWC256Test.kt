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
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import it.neckar.open.random.MWC256
import org.junit.jupiter.api.Test

class MWC256Test {
  /**
   * The internal ring buffer index is derived from a Byte counter. When the mask is applied on the
   * Byte (AND with -1) instead of on the Int (AND with 0xFF), the sign-extended index turns negative
   * once the Byte overflows from 127 to -128 — the 129th draw indexes q[-128] and crashes.
   */
  @Test
  fun `draws beyond the byte boundary do not crash`() {
    val random = MWC256()

    val values = (1..1000).map { random.get() }

    assertThat(values).hasSize(1000)
  }

  /**
   * The recurrence works on unsigned 32-bit values. Reading `q[idx]` as a signed `Int` makes
   * `t` negative for state values >= 2^31, and the arithmetic shift `t shr 32` then produces a
   * wrong carry — the sequence diverges from the MWC256 recurrence.
   */
  @Test
  fun `recurrence matches the MWC256 reference computed in unsigned arithmetic`() {
    val random = MWC256()
    val reference = UnsignedMwc256Reference(random.q, random.c)

    repeat(10_000) { draw ->
      assertThat(random.get().toUInt(), name = "draw #$draw").isEqualTo(reference.next())
    }
  }
}

/**
 * Independent oracle for Marsaglia's MWC256 recurrence with base `b = 2^32`,
 * computed in `ULong` so no signed truncation can occur:
 *
 * ```
 * t_n = a * x_(n-256) + c_(n-1)
 * c_n = t_n / b
 * x_n = t_n mod b
 * ```
 */
private class UnsignedMwc256Reference(seededState: IntArray, seededCarry: Int) {
  private val multiplier: ULong = 809430660uL
  private val state: ULongArray = ULongArray(seededState.size) { index -> seededState[index].toUInt().toULong() }
  private var carry: ULong = seededCarry.toULong()
  private var index: Int = 255

  fun next(): UInt {
    index = (index + 1) and 255
    val t: ULong = multiplier * state[index] + carry
    carry = t shr 32
    state[index] = t and 4294967295uL
    return state[index].toUInt()
  }
}
