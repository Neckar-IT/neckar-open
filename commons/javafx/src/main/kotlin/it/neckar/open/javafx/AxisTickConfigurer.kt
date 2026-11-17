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
package it.neckar.open.javafx

import javafx.beans.binding.Bindings
import javafx.scene.chart.NumberAxis

/**
 * GlobalTilesCache * @see AxisConfigurer
 */
object AxisTickConfigurer {
  /**
   * Computes the tick unit in accordance to the current [NumberAxis.lowerBoundProperty] and [NumberAxis.upperBoundProperty] of the given axis.
   */
  @JvmStatic
  fun bindTickUnitToBounds(axis: NumberAxis) {
    axis.tickUnitProperty().bind(Bindings.createDoubleBinding({
      val lowerBound = axis.lowerBoundProperty().get()
      val upperBound = axis.upperBoundProperty().get()
      if (java.lang.Double.isNaN(lowerBound) || java.lang.Double.isNaN(upperBound)) {
        return@createDoubleBinding 1.0
      }
      val range = upperBound - lowerBound
      getTickCount(range)
    }, axis.lowerBoundProperty(), axis.upperBoundProperty()))
  }

  @JvmStatic
  fun getTickCount(range: Double): Double {
    val positiveRange = Math.abs(range)
    if (positiveRange == 0.0) {
      return 1.0
    }
    val powerOfTen = Math.pow(10.0, Math.floor(Math.log10(positiveRange))).toInt() // 3145 -> 3000, 27 -> 10, 534 -> 100 etc
    val limit = (positiveRange / powerOfTen).toInt() * powerOfTen
    return Math.max(1.0, limit * 0.1)
  }

  @JvmStatic
  fun configureAxisTick(axis: NumberAxis, upperLimit: Double) {
    axis.tickUnit = getTickCount(upperLimit)
  }
}
