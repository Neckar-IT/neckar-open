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
import assertk.assertions.isEqualTo
import it.neckar.open.random.CMWC4096
import org.junit.jupiter.api.Test

class CMWC4096Test {
  /**
   * The recurrence works on unsigned 32-bit values. Truncating the complement constant
   * `r = 0xFFFFFFFE` to `Int` (`-2`) makes the carry test `t > r` always true, and reading
   * `q[i]` as a signed `Int` breaks the multiplication for values >= 2^31. Both deviations
   * make the output diverge from the CMWC4096 recurrence.
   */
  @Test
  fun `recurrence matches the CMWC4096 reference computed in unsigned arithmetic`() {
    val random = CMWC4096()
    val reference = UnsignedCmwc4096Reference(random.q, random.c)

    repeat(10_000) { draw ->
      assertThat(random.get().toUInt(), name = "draw #$draw").isEqualTo(reference.next())
    }
  }
}

/**
 * Independent oracle for Marsaglia's CMWC4096 recurrence with base `b = 2^32 - 1`,
 * computed in `ULong` so no signed truncation can occur:
 *
 * ```
 * t_n = a * x_(n-4096) + c_(n-1)
 * c_n = t_n / b
 * x_n = (b - 1) - (t_n mod b)
 * ```
 *
 * Uses plain division/modulo instead of the carry folding of the production code,
 * so both implementations only agree when they compute the same recurrence.
 */
private class UnsignedCmwc4096Reference(seededState: IntArray, seededCarry: Int) {
  private val base: ULong = 4294967295uL
  private val multiplier: ULong = 18782uL
  private val state: ULongArray = ULongArray(seededState.size) { index -> seededState[index].toUInt().toULong() }
  private var carry: ULong = seededCarry.toULong()
  private var index: Int = 0

  fun next(): UInt {
    index = (index + 1) and 4095
    val t: ULong = multiplier * state[index] + carry
    carry = t / base
    state[index] = (base - 1uL) - (t % base)
    return state[index].toUInt()
  }
}
