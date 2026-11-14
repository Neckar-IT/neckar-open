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

open class CMWC4096 : BasePseudoRandomNumberGenerator() {

  var c = 0
  var q = IntArray(4096)
  var lcg = LinearCongruentialGenerator()
  var i = 0

  init {
    setSeed(InitSeed)
  }

  override fun get(): Int {
    val a = 18782L
    val b = 4294967295L
    val r: Long = b - 1
    i = i + 1 and 4095
    //Read the state as unsigned 32-bit value: a signed read breaks the multiplication for values >= 2^31.
    var t: Long = a * (q[i].toLong() and b) + c
    c = (t shr IntBits).toInt()
    t = (t and b) + c
    if (t > r) {
      c++
      t -= b
    }
    q[i] = (r - t).toInt()
    return q[i]
  }

  override fun setSeed(seed: Long) {
    lcg.setSeed(seed)
    for (i in 0..4095) {
      q[i] = lcg.get()
    }
    c = lcg.getTarget(18781)
  }

  companion object {
    private const val InitSeed = 10000L
    private const val IntBits = 32
  }

}
