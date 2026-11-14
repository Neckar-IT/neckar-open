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
package it.neckar.open.health

import kotlin.jvm.JvmInline

/**
 * Counts the starts of one background job, so a record from a cancelled start is told apart from one
 * of the start that replaced it under the same name.
 */
@JvmInline
value class BackgroundJobRun(
  /**
   * `1` for the first start of the job in this process.
   */
  val number: Int,
) {
  init {
    require(number >= 1) { "run number must be at least 1 but was $number" }
  }

  fun next(): BackgroundJobRun = BackgroundJobRun(number + 1)

  override fun toString(): String = number.toString()

  companion object {
    val First: BackgroundJobRun = BackgroundJobRun(1)
  }
}
