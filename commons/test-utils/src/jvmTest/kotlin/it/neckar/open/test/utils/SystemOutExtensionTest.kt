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
package it.neckar.open.test.utils

import assertk.*
import assertk.assertions.*

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.annotation.Nonnull

/**
 *
 */
@ExtendWith(SystemOutExtension::class)
class SystemOutExtensionTest {

  private lateinit var n: String

  @BeforeEach
  fun setUp() {
    n = System.getProperty("line.separator")
    assertThat(n.length).isGreaterThanOrEqualTo(1)
  }

  @Test
  fun testIt(@Nonnull systemOutExtension: SystemOutExtension) {
    println("Hey")
    println("2")
    assertEquals("Hey" + n + "2" + n + "", systemOutExtension.outAsString)
    println("3")
    assertEquals("Hey" + n + "2" + n + "3" + n + "", systemOutExtension.outAsString)
  }

  @Test
  fun testOut2(@Nonnull systemOutExtension: SystemOutExtension) {
    println("2")
    assertEquals("2$n", systemOutExtension.outAsString)
    println("3")
    assertEquals("2" + n + "3" + n + "", systemOutExtension.outAsString)
  }

  @Test
  fun testErr(@Nonnull systemOutExtension: SystemOutExtension) {
    System.err.println("Hey")
    System.err.println("2")
    assertEquals("Hey" + n + "2" + n + "", systemOutExtension.errAsString)
    System.err.println("3")
    assertEquals("Hey" + n + "2" + n + "3" + n + "", systemOutExtension.errAsString)
  }
}
