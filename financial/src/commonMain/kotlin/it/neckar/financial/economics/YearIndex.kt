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
package it.neckar.financial.economics

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents a relative year
 */
@Serializable
@JvmInline
value class YearIndex(val index: Int) {
  init {
    require(index >= 0) { "Invalid index <$index>" }
  }

  operator fun minus(i: Int): YearIndex {
    return YearIndex(index - i)
  }

  /**
   * A human-readable index (1 based)
   */
  val indexBase1: Int
    get() {
      return index + 1
    }

  companion object {
    /**
     * The first or base year
     */
    val base: YearIndex = YearIndex(0)
    val second: YearIndex = YearIndex(1)
    val third: YearIndex = YearIndex(2)
    val fourth: YearIndex = YearIndex(3)
  }
}
