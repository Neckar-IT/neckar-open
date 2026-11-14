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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.open.unit.utils.prefix

import assertk.*
import assertk.assertions.*
import it.neckar.open.unit.prefix.centi
import it.neckar.open.unit.prefix.deci
import it.neckar.open.unit.prefix.kilo
import it.neckar.open.unit.si.cm
import it.neckar.open.unit.si.m
import it.neckar.open.unit.si.mm
import it.neckar.open.unit.utils.prefix.Prefixed.getFactor
import it.neckar.open.unit.utils.prefix.Prefixed.findPrefixAnnotation
import it.neckar.open.unit.utils.prefix.Prefixed.isPrefixed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


/**
 */
//@Disabled
class PrefixedTest {
  @Test
  fun testFactor() {
    assertEquals(0.01, getFactor(centi::class.java), 0.0)
    assertEquals(0.1, getFactor(deci::class.java), 0.0)
    assertEquals(1000.0, getFactor(kilo::class.java), 0.0)
  }

  @Test
  fun testFactor2() {
    assertEquals(0.01, getFactor(cm::class.java), 0.0)
    assertEquals(0.001, getFactor(mm::class.java), 0.0)
    assertFailure {
      getFactor(m::class.java)
    }.isInstanceOf(IllegalArgumentException::class).hasMessage("[it.neckar.open.unit.si.m] is not annotated with [it.neckar.open.unit.prefix.Prefix]")
  }

  @Test
  @Throws(Exception::class)
  fun testIsPrefixed() {
    //the prefix annotation itself
    assertThat(isPrefixed(centi::class.java)).isTrue()

    //units that carry a prefix annotation
    assertThat(isPrefixed(cm::class.java)).isTrue()
    assertThat(isPrefixed(mm::class.java)).isTrue()
    assertThat(isPrefixed(m::class.java)).isFalse()
  }

  @Test
  @Throws(Exception::class)
  fun testPrefix() {
    assertThat(findPrefixAnnotation(centi::class.java)?.value).isEqualTo(0.01)

    assertThat(findPrefixAnnotation(cm::class.java)?.value).isEqualTo(0.01)
    assertThat(findPrefixAnnotation(mm::class.java)?.value).isEqualTo(0.001)
    assertThat(findPrefixAnnotation(m::class.java)).isNull()
  }
}
