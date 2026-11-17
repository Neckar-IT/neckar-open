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

import it.neckar.open.unit.si.microM
import it.neckar.open.unit.si.mm
import it.neckar.open.unit.si.ns
import it.neckar.open.unit.si.s
import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.DoubleProperty

/**
 * TODO: Merge with unit stuff
 */
object Units {
  @JvmStatic
  fun microMeter2mm(doubleProperty: DoubleProperty): DoubleBinding {
    return Bindings.createDoubleBinding({ microMeter2mm(doubleProperty.get()) }, doubleProperty)
  }

  @JvmStatic
  fun mm2microMeter(doubleProperty: DoubleProperty): DoubleBinding {
    return Bindings.createDoubleBinding({ mm2microMeter(doubleProperty.get()) }, doubleProperty)
  }

  /**
   * Converts a micro meter value to milli meter
   */
  @JvmStatic
  fun microMeter2mm(value: @microM Double): @mm Double {
    if (value == Double.MIN_VALUE) {
      return Double.MIN_VALUE
    }
    return if (value == -Double.MIN_VALUE) {
      -Double.MIN_VALUE
    } else value / 1000.0
  }

  /**
   * Converts a milli meter value to micro meter
   */
  @JvmStatic
  fun mm2microMeter(value: @mm Double): @microM Double {
    if (value == Double.MIN_VALUE) {
      return Double.MIN_VALUE
    }
    return if (value == -Double.MIN_VALUE) {
      -Double.MIN_VALUE
    } else value * 1000.0
  }

  /**
   * Converts nanoseconds to seconds as double
   */
  @JvmStatic
  fun nanos2secs(value: @ns Long): @s Double {
    return value / 1000.0 / 1000.0 / 1000.0
  }
}
