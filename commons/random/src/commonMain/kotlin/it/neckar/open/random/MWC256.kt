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
package it.neckar.open.random

/**
 * Multiply with carry.
 */
open class MWC256 : BasePseudoRandomNumberGenerator() {

  var q: IntArray = IntArray(ByteMaxIndices)
  var c: Int = 0
  var lcg: LinearCongruentialGenerator = LinearCongruentialGenerator()
  var b: Byte = ff

  init {
    setSeed(InitSeed)
  }

  override fun get(): Int {
    //Mask on the Int, not the Byte: the ring index must span 0..255. Masking on the Byte (AND with
    //FF == -1) is a no-op and the sign-extended index turns negative once b overflows 127 -> -128.
    val idx = (++b).toInt() and UbyteMax
    //Read the state as unsigned 32-bit value: a signed read makes t negative for values >= 2^31,
    //and the arithmetic shift below then produces a wrong carry.
    val t: Long = Init * (q[idx].toLong() and Mask) + c
    c = (t shr IntBits).toInt()
    q[idx] = t.toInt()
    return t.toInt()
  }

  override fun setSeed(seed: Long) {
    lcg.setSeed(seed)
    for (i in 0..UbyteMax) {
      q[i] = lcg.get()
    }
    c = lcg.getTarget(Init.toInt())
  }

  companion object {
    private const val Init = 809430660L
    private const val Mask = 0xffffffffL
    private const val InitSeed = 10000L
    private const val IntBits = 32
    private const val UbyteMax = 255
    private const val ff = UbyteMax.toByte()
    private const val ByteMaxIndices = 256
  }
}
