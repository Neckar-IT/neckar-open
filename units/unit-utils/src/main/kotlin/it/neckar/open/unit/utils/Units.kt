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
package it.neckar.open.unit.utils

import it.neckar.open.kotlin.lang.getAnnotationNonNull
import it.neckar.open.unit.Definition
import it.neckar.open.unit.Name
import it.neckar.open.unit.Symbol
import it.neckar.open.unit.Unit
import it.neckar.open.unit.si.SIBaseUnit
import it.neckar.open.unit.si.SiDerivedUnit
import kotlin.reflect.KClass

/**
 * Helper class for Units
 *
 */
object Units {
  /**
   * Returns the symbol for the given unit type
   *
   * @param unitType the unit type
   * @return the symbol
   */
  @JvmStatic
  fun getSymbol(unitType: Class<out Annotation>): String {
    val symbol = unitType.getAnnotationNonNull(Symbol::class.java)
    return symbol.value
  }

  /**
   * Returns true if this is a unit annotation
   */
  fun Class<out Annotation>.isUnit(): Boolean {
    return this.getAnnotation<Unit>(Unit::class.java) != null
  }

  /**
   * Returns the name
   *
   * @param unitType the unit type
   * @return the name
   */
  @JvmStatic
  fun getName(unitType: Class<out Annotation>): String {
    val name = unitType.getAnnotationNonNull(Name::class.java)
    return name.value
  }

  /**
   * Whether the given unit type is a SI unit
   *
   * @param this@isSiBaseUnit the unit type
   * @return true if this is a si base unit, false otherwise
   */
  @JvmStatic
  fun Class<out Annotation>.isSiBaseUnit(): Boolean {
    return this.getAnnotation(SIBaseUnit::class.java) != null
  }

  /**
   * Whether it is a derived si unit
   *
   * @param unitType the unit type
   * @return true if the given unit type is a derived si unit
   */
  @JvmStatic
  fun isDerivedSiUnit(unitType: Class<out Annotation>): Boolean {
    return unitType.getAnnotation(SiDerivedUnit::class.java) != null
  }

  @JvmStatic
  fun getDerivedFrom(unitType: Class<out Annotation>): List<KClass<out Annotation>> {
    val derived = unitType.getAnnotationNonNull(SiDerivedUnit::class.java)
    return derived.value.toList()
  }

  @JvmStatic
  fun getDefinitions(unitType: Class<out Annotation>): List<String> {
    val definition = unitType.getAnnotationNonNull(Definition::class.java)
    return definition.value.toList()
  }
}

