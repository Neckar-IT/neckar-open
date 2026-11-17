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

import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.scene.chart.NumberAxis
import javax.annotation.Nonnull

/**
 * @see AxisTickConfigurer
 */
object AxisConfigurer {
  /**
   * Sets the lower bound of the given axis in accordance to the given property.
   */
  @JvmStatic
  fun bindLowerBound(@Nonnull axis: NumberAxis, @Nonnull numberProperty: Property<Number?>) {
    numberProperty.addListener { observable: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? -> decreaseLowerBound(axis, newValue) }
    decreaseLowerBound(axis, numberProperty.value)
  }

  /**
   * Sets the upper bound of the given axis in accordance to the given property.
   */
  @JvmStatic
  fun bindUpperBound(@Nonnull axis: NumberAxis, @Nonnull numberProperty: Property<Number?>) {
    numberProperty.addListener { observable: ObservableValue<out Number?>?, oldValue: Number?, newValue: Number? -> increaseUpperBound(axis, newValue) }
    increaseUpperBound(axis, numberProperty.value)
  }

  /**
   * Sets the lower bound of the given axis to it includes at least the given maximum value.
   */
  @JvmStatic
  fun decreaseLowerBound(@Nonnull axis: NumberAxis, maxValue: Number?) {
    if (maxValue == null) {
      return
    }
    val d = maxValue.toDouble()
    val maxValueMinus1Percent = d - Math.abs(d * 0.01)
    if (maxValueMinus1Percent < axis.lowerBound) {
      val maxValueMinus10Percent = d - Math.abs(d * 0.1)
      val tickUnit = AxisTickConfigurer.getTickCount(maxValueMinus10Percent)
      axis.lowerBound = Math.floor(maxValueMinus10Percent / tickUnit) * tickUnit
    }
  }

  /**
   * Sets the upper bound of the given axis to it includes at least the given minimum value.
   */
  @JvmStatic
  fun increaseUpperBound(@Nonnull axis: NumberAxis, minValue: Number?) {
    if (minValue == null) {
      return
    }
    val d = minValue.toDouble()
    val minValuePlus1Percent = d + Math.abs(d * 0.01)
    if (minValuePlus1Percent > axis.upperBound) {
      val minValuePlus10Percent = d + Math.abs(d * 0.1)
      val tickUnit = AxisTickConfigurer.getTickCount(minValuePlus10Percent)
      axis.upperBound = Math.ceil(minValuePlus10Percent / tickUnit) * tickUnit
    }
  }
}
