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

import assertk.*
import assertk.assertions.*
import it.neckar.open.unit.other.AM
import it.neckar.open.unit.prefix.hecto
import it.neckar.open.unit.si.A
import it.neckar.open.unit.si.C
import it.neckar.open.unit.si.K
import it.neckar.open.unit.si.degC
import it.neckar.open.unit.si.m
import it.neckar.open.unit.si.m2
import it.neckar.open.unit.si.m_s2
import it.neckar.open.unit.si.s
import it.neckar.open.unit.utils.Units.getDefinitions
import it.neckar.open.unit.utils.Units.getDerivedFrom
import it.neckar.open.unit.utils.Units.getSymbol
import it.neckar.open.unit.utils.Units.isDerivedSiUnit
import it.neckar.open.unit.utils.Units.isSiBaseUnit
import it.neckar.open.unit.utils.Units.isUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Copyright (C) cedarsoft GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 */
class UnitsTest {
  //@Disabled
  @Test
  fun testToString() {
    assertEquals("m", getSymbol(m::class.java))
  }

  @Test
  fun testIsUnit() {
    assertThat(m::class.java.isUnit()).isTrue()
    assertThat(m2::class.java.isUnit()).isTrue()
    assertThat(hecto::class.java.isUnit()).isFalse()
  }

  @Test
  fun testSymbol() {
    assertEquals("m", getSymbol(m::class.java))
    assertEquals("m²", getSymbol(m2::class.java))
    assertEquals("m/s²", getSymbol(m_s2::class.java))
  }

  @Test
  fun testName() {
    assertEquals("metre", Units.getName(m::class.java))
    assertEquals("square metre", Units.getName(m2::class.java))
    assertEquals("metre per square second", Units.getName(m_s2::class.java))
  }

  @Test
  fun testSiBase() {
    assertThat(m::class.java.isSiBaseUnit()).isTrue()
    assertThat(A::class.java.isSiBaseUnit()).isTrue()
    assertThat(degC::class.java.isSiBaseUnit()).isFalse()
    assertThat(C::class.java.isSiBaseUnit()).isFalse()
  }

  @Test
  fun testDerived() {
    assertFalse(isDerivedSiUnit(m::class.java))
    assertFalse(isDerivedSiUnit(A::class.java))
    assertTrue(isDerivedSiUnit(degC::class.java))
    assertTrue(isDerivedSiUnit(C::class.java))
  }

  @Test
  fun testDefinition() {
    assertThat(getDefinitions(m::class.java)).containsExactly(
      "distance travelled by light in vacuum in 1⁄299,792,458 of a second",
      "Obsolete: ten-millionth of the distance from the Earth's equator to the North Pole (at sea level)",
    )
    assertThat(getDefinitions(AM::class.java)).containsExactly(
      "optical path length through Earth's atmosphere",
      "relative to path length to that at the zenith at sea level",
    )
    assertThat(getDefinitions(degC::class.java)).containsExactly("[K]-273.15")
  }

  @Test
  fun testDefinitionMissing() {
    assertFailure {
      getDefinitions(A::class.java)
    }.hasMessage("Annotation [it.neckar.open.unit.Definition] not found for [interface it.neckar.open.unit.si.A]")
  }

  @Test
  fun testDerivedFrom() {
    assertFailure {
      getDerivedFrom(m::class.java)
    }.hasMessage("Annotation [it.neckar.open.unit.si.SiDerivedUnit] not found for [interface it.neckar.open.unit.si.m]")

    assertThat(getDerivedFrom(degC::class.java)).containsExactly(K::class)
    assertThat(getDerivedFrom(C::class.java)).containsExactly(A::class, s::class)
  }
}
