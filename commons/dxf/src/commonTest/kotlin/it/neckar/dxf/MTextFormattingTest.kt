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
package it.neckar.dxf

import kotlin.test.Test
import kotlin.test.assertEquals

class MTextFormattingTest {

  @Test
  fun plainTextIsUnchanged() {
    assertEquals("Küche", cleanMTextFormatting("Küche"))
  }

  @Test
  fun removesFontBlockAndConvertsParagraph() {
    val raw = """{\fBahnschrift Light|b0|i0|c0|p34;Wohnen/Essen\P41.90m²}"""
    assertEquals("Wohnen/Essen\n41.90m²", cleanMTextFormatting(raw))
  }

  @Test
  fun removesParagraphPropertyAndConvertsTab() {
    val raw = """\pxql;FUSSBODEN\PFußbodenfertigbelag^I10mm"""
    assertEquals("FUSSBODEN\nFußbodenfertigbelag\t10mm", cleanMTextFormatting(raw))
  }

  @Test
  fun removesHeightCode() {
    assertEquals("Höhe klein", cleanMTextFormatting("""Höhe \H0.8x;klein"""))
  }

  @Test
  fun keepsEscapedBraces() {
    assertEquals("a{b}c", cleanMTextFormatting("""a\{b\}c"""))
  }
}
