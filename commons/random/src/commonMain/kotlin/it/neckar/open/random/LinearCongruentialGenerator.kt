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
 * Linear congruential generator.
 * https://en.wikipedia.org/wiki/Linear_congruential_generator
 */
open class LinearCongruentialGenerator : BasePseudoRandomNumberGenerator() {

  var state: Long = 0

  init {
    setSeed(InitSeed)
  }

  @Suppress("MagicNumber")
  override fun get(): Int {
    state = (25214903917L * state + 11) % 281474976710656L
    return state.toInt()
  }

  override fun setSeed(seed: Long) {
    state = seed.toInt().toLong()
  }

  companion object {
    private const val InitSeed = 10000L
  }
}
